package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource : ReminderDataSource {

    var shouldReturnError = false
    var reminders = mutableListOf<ReminderDTO>()

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test Error: getReminders")
        } else {
            return Result.Success(reminders)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test Error: getReminder")
        } else {
            val reminder = reminders.find{
                it.id == id
            }
            reminder?.let {
                return Result.Success(reminder)
            }
        }
        return Result.Error("Test Error: getReminder for id $id not found")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}