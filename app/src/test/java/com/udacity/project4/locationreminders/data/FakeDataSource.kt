package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var shouldReturnError = false
    var reminders = mutableListOf<ReminderDTO>()

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error("Error on getReminders")
        } else {
            Result.Success(reminders)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Error on getReminder")
        } else {
            val reminder = reminders.find{
                it.id == id
            }
            reminder?.let {
                return Result.Success(reminder)
            }
        }
        return Result.Error("Error: not found with id => $id")

    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}