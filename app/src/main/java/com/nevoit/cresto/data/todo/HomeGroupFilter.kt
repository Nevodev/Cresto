package com.nevoit.cresto.data.todo

sealed interface HomeGroupFilter {
    data object All : HomeGroupFilter
    data object Ungrouped : HomeGroupFilter
    data class Group(val id: Int) : HomeGroupFilter
}
