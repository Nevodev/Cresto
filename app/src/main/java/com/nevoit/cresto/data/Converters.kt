package com.nevoit.cresto.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let {
            return LocalDateTime.parse(it, dateTimeFormatter)
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun fromDateStamp(value: String?): LocalDate? {
        return value?.let {
            return LocalDate.parse(it, dateFormatter)
        }
    }

    @TypeConverter
    fun dateToDateStamp(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }
}
