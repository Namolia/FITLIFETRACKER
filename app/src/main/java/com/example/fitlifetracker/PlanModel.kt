package com.example.fitlifetracker

data class Plan(
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val price: Long = 0,
    val stock: Long = 0,
    val onSale: Boolean = true,
    val imageUrl: String = ""

)
