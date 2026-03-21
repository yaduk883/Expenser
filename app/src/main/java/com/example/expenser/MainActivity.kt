package com.example.expenser

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class MainActivity : AppCompatActivity() {

    private var selectedDate: Long = System.currentTimeMillis()
    private var editingExpense: Expense? = null
    private var selectedProfileId: Long = -1L

    private val categoryMap = mutableMapOf(
        "Food & groceries" to mutableListOf("Eating out / ordering food", "Groceries"),
        "House rent" to mutableListOf("Monthly Rent", "Maintenance"),
        "Bills" to mutableListOf("Electricity", "Water", "Gas bills"),
        "Transportation" to mutableListOf("Fuel", "Bus", "Metro", "Train", "Cab"),
        "Loan EMIs" to mutableListOf("Home Loan", "Car loan", "Personal Loan", "Educational Loan"),
        "Insurance premiums" to mutableListOf("Health insurance", "Vehicle Insurances"),
        "Taxes" to mutableListOf("Income Tax", "Property Tax"),
        "Shopping" to mutableListOf("Cloths", "Gadgets"),
        "Entertainment" to mutableListOf("Movies", "Subscriptions", "Recharges"),
        "Savings" to mutableListOf("Bank savings", "Retirement funds", "Mutual funds / stocks"),
        "Education" to mutableListOf("School / college fees", "Online courses", "Books and study materials"),
        "Health & Medical" to mutableListOf("Doctor visits", "Medicines"),
        "Unexpected / Emergency Expenses" to mutableListOf("Repairs (vehicle, home)", "Medical emergencies", "Sudden travel needs"),
        "Vaccation" to mutableListOf("Tours", "Long trips")
    )

    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = ExpenseDatabase.getDatabase(this)

        val spinnerHead = findViewById<Spinner>(R.id.spinnerHead)
        val spinnerSub = findViewById<Spinner>(R.id.spinnerSubHead)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val tvDate = findViewById<TextView>(R.id.tvDateDisplay)
        val btnAddProfile = findViewById<ImageButton>(R.id.btnAddProfile)
        val btnAddCategory = findViewById<ImageButton>(R.id.btnAddCategory)
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)

        val tvSelectProfile = findViewById<TextView>(R.id.tvSelectProfile)
        val tvRecentExpensesHeader = findViewById<TextView>(R.id.tvRecentExpensesHeader)

        // 1. Profile Setup & Toggle
        val rvProfiles = findViewById<RecyclerView>(R.id.rvProfiles)
        profileAdapter = ProfileAdapter(
            emptyList(),
            onProfileClick = { profile ->
                selectedProfileId = profile.id
                Toast.makeText(this, "Selected: ${profile.name}", Toast.LENGTH_SHORT).show()
            },
            onProfileLongClick = { profile ->
                if (profile.name != "Self") {
                    showDeleteProfileDialog(db, profile)
                }
            }
        )
        rvProfiles.adapter = profileAdapter

        tvSelectProfile.setOnClickListener {
            rvProfiles.visibility = if (rvProfiles.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 2. Recent Expenses Setup (Edit & Delete Logic Included)
        val rvRecentExpenses = findViewById<RecyclerView>(R.id.rvRecentExpenses)
        expenseAdapter = ExpenseAdapter(
            emptyList(),
            onItemClick = { expense ->
                // Load data for editing
                editingExpense = expense
                etAmount.setText((expense.amountCents / 100f).toString())

                val heads = categoryMap.keys.toList()
                val headPos = heads.indexOf(expense.category)
                if (headPos >= 0) spinnerHead.setSelection(headPos)

                selectedDate = expense.date.toEpochMilli()
                tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))

                btnSave.text = "Update Expense"
                Toast.makeText(this, "Edit Mode Enabled", Toast.LENGTH_SHORT).show()
            },
            onItemLongClick = { expense ->
                showDeleteExpenseDialog(db, expense)
            }
        )
        rvRecentExpenses.adapter = expenseAdapter

        tvRecentExpensesHeader.setOnClickListener {
            rvRecentExpenses.visibility = if (rvRecentExpenses.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // 3. Load Profiles
        lifecycleScope.launch {
            db.expenseDao().getAllProfiles().collectLatest { profiles ->
                if (profiles.isEmpty()) {
                    db.expenseDao().insertProfile(Profile(name = "Self", age = 25, type = ProfileType.USER))
                } else {
                    if (selectedProfileId == -1L) {
                        selectedProfileId = profiles[0].id
                    }
                }
                profileAdapter.updateProfiles(profiles)
            }
        }

        // 4. Spinner Setup
        updateMainSpinner(spinnerHead)
        spinnerHead.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val selectedHead = categoryMap.keys.toList()[pos]
                val subList = categoryMap[selectedHead] ?: mutableListOf()
                spinnerSub.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, subList)

                // Select the sub-category if in editing mode
                editingExpense?.let {
                    val subPos = subList.indexOf(it.subCategory)
                    if (subPos >= 0) spinnerSub.setSelection(subPos)
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 5. Date Picker & Menu Buttons
        btnAddProfile.setOnClickListener { showAddProfileDialog(db) }
        btnAddCategory.setOnClickListener { showAddCategoryDialog(spinnerHead) }
        btnMenu.setOnClickListener { showMainMenu(db, pieChart) }

        findViewById<Button>(R.id.btnPickDate).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDate = cal.timeInMillis
                tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 6. Save/Update Expense Logic
        btnSave.setOnClickListener {
            val rawAmount = etAmount.text.toString()
            if (rawAmount.isEmpty()) {
                Toast.makeText(this, "Enter Amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedProfileId == -1L) {
                Toast.makeText(this, "Select Profile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amountInPaise = try {
                (rawAmount.toDouble() * 100).toLong()
            } catch (e: Exception) {
                0L
            }

            lifecycleScope.launch(Dispatchers.IO) {
                if (editingExpense != null) {
                    // Update current record
                    val updated = editingExpense!!.copy(
                        amountCents = amountInPaise,
                        category = spinnerHead.selectedItem.toString(),
                        subCategory = spinnerSub.selectedItem.toString(),
                        date = Instant.ofEpochMilli(selectedDate),
                        profileId = selectedProfileId
                    )
                    db.expenseDao().update(updated)
                    editingExpense = null
                    launch(Dispatchers.Main) { btnSave.text = "Save Expense" }
                } else {
                    // Insert new record
                    val expense = Expense(
                        amountCents = amountInPaise,
                        category = spinnerHead.selectedItem.toString(),
                        subCategory = spinnerSub.selectedItem.toString(),
                        date = Instant.ofEpochMilli(selectedDate),
                        profileId = selectedProfileId
                    )
                    db.expenseDao().insert(expense)
                }

                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Saved Succesfully", Toast.LENGTH_SHORT).show()
                    etAmount.text.clear()
                }
            }
        }

        // 7. Data Observers
        lifecycleScope.launch {
            db.expenseDao().getAllExpenses().collectLatest { list ->
                expenseAdapter.updateList(list)
                updateChart(pieChart, list)
            }
        }
    }

    private fun showMainMenu(db: ExpenseDatabase, pieChart: PieChart) {
        val options = arrayOf("View Reports", "Compare Months", "Settings", "Help", "Contact")
        AlertDialog.Builder(this)
            .setTitle("Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showViewReportDialog(db, pieChart)
                    1 -> showCompareMonthsPicker(db)
                    2 -> Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(this, "Help center", Toast.LENGTH_SHORT).show()
                    4 -> showContactDialog()
                }
            }.show()
    }

    private fun showContactDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Contact Developer")

        // Helper to convert pixels to dp
        fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Using 24dp for horizontal and 16dp for vertical padding is standard
            setPadding(24.toDp(), 16.toDp(), 24.toDp(), 16.toDp())
        }

        fun createContactButton(labelText: String, url: String, isEmail: Boolean = false): Button {
            // MaterialButton looks better and handles clicks with a ripple effect
            return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle).apply {
                text = labelText
                setOnClickListener {
                    try {
                        val intent = if (isEmail) {
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$url")
                            }
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback if no app is found to handle the intent
                        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 4.toDp(), 0, 4.toDp()) // Smaller margin for a tighter look
        }

        layout.addView(createContactButton("📧 Email", "yaduk883@gmail.com", true), params)
        layout.addView(createContactButton("🐙 GitHub", "https://github.com/yaduk883"), params)
        layout.addView(createContactButton("📸 Instagram", "https://instagram.com/ig.yadu/"), params)

        builder.setView(layout)
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun showViewReportDialog(db: ExpenseDatabase, pieChart: PieChart) {
        val reportOptions = arrayOf("Monthly Report", "Yearly Report", "Full History")
        AlertDialog.Builder(this)
            .setTitle("View Reports")
            .setItems(reportOptions) { _, which ->
                when (which) {
                    0 -> showMonthPicker(db, pieChart)
                    1 -> showYearPicker(db, pieChart)
                    2 -> {
                        lifecycleScope.launch {
                            db.expenseDao().getAllExpenses().collectLatest { list ->
                                expenseAdapter.updateList(list)
                                updateChart(pieChart, list)
                            }
                        }
                    }
                }
            }.show()
    }

    private fun showCompareMonthsPicker(db: ExpenseDatabase) {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        var m1 = -1
        AlertDialog.Builder(this)
            .setTitle("Select First Month")
            .setItems(months) { _, which ->
                m1 = which
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Select Second Month")
                    .setItems(months) { _, which2 -> compareData(db, m1, which2) }.show()
            }.show()
    }

    private fun compareData(db: ExpenseDatabase, m1: Int, m2: Int) {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        lifecycleScope.launch {
            val list = db.expenseDao().getAllExpenses().first()

            val year = Calendar.getInstance().get(Calendar.YEAR)

            val total1 = list.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.date.toEpochMilli() }
                c.get(Calendar.MONTH) == m1 && c.get(Calendar.YEAR) == year
            }.sumOf { it.amountCents } / 100f

            val total2 = list.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.date.toEpochMilli() }
                c.get(Calendar.MONTH) == m2 && c.get(Calendar.YEAR) == year
            }.sumOf { it.amountCents } / 100f

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Comparison Results")
                .setMessage("${months[m1]}: ₹$total1\n${months[m2]}: ₹$total2\n\nDifference: ₹${Math.abs(total1 - total2)}")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showMonthPicker(db: ExpenseDatabase, pieChart: PieChart) {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        AlertDialog.Builder(this)
            .setTitle("Select Month")
            .setItems(months) { _, which -> filterData(db, pieChart, which, Calendar.getInstance().get(Calendar.YEAR)) }
            .show()
    }

    private fun showYearPicker(db: ExpenseDatabase, pieChart: PieChart) {
        val years = arrayOf("2024", "2025", "2026")
        AlertDialog.Builder(this)
            .setTitle("Select Year")
            .setItems(years) { _, which -> filterData(db, pieChart, -1, years[which].toInt()) }
            .show()
    }

    private fun filterData(db: ExpenseDatabase, pieChart: PieChart, month: Int, year: Int) {
        lifecycleScope.launch {
            db.expenseDao().getAllExpenses().collectLatest { all ->
                val filtered = all.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date.toEpochMilli() }
                    if (month == -1) cal.get(Calendar.YEAR) == year
                    else cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
                }
                expenseAdapter.updateList(filtered)
                updateChart(pieChart, filtered)
            }
        }
    }

    private fun updateMainSpinner(spinnerHead: Spinner) {
        spinnerHead.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryMap.keys.toList())
    }

    private fun showAddProfileDialog(db: ExpenseDatabase) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("N")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etName = EditText(this).apply { hint = "പേര്" }
        layout.addView(etName)
        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val name = etName.text.toString()
            if (name.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    db.expenseDao().insertProfile(Profile(name = name, age = 0, type = ProfileType.USER))
                }
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showAddCategoryDialog(spinnerHead: Spinner) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Category")
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etNewHead = EditText(this).apply { hint = "New Main Category" }
        val etNewSub = EditText(this).apply { hint = "New Sub Category" }
        layout.addView(etNewHead)
        layout.addView(etNewSub)
        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val h = etNewHead.text.toString().trim()
            val s = etNewSub.text.toString().trim()
            if (h.isNotEmpty()) {
                categoryMap[h] = mutableListOf(if (s.isNotEmpty()) s else "General")
                updateMainSpinner(spinnerHead)
            }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showDeleteProfileDialog(db: ExpenseDatabase, profile: Profile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Profile")
            .setMessage("'${profile.name}' Do You Want To Delete?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) { db.expenseDao().deleteProfile(profile) }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showDeleteExpenseDialog(db: ExpenseDatabase, expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Do You Want To Delete This Record?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) { db.expenseDao().delete(expense) }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateChart(chart: PieChart, list: List<Expense>) {
        val entries = list.groupBy { it.category }
            .map { (cat, exps) -> PieEntry(exps.sumOf { it.amountCents } / 100f, cat) }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        chart.apply {
            data = PieData(dataSet)
            animateXY(800, 800)
            invalidate()
        }
    }
}
