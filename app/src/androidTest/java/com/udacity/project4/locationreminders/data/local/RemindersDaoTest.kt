package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var db: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun cleanupDb() {
        db.close()
    }

    @Test
    fun saveReminder() = runBlockingTest {
        db.reminderDao()
            .saveReminder(
                ReminderDTO(
                    "Title Test",
                    "Description Test",
                    "Location Test",
                    0.0,
                    0.0
                )
            )

        val retrievedReminder = db.reminderDao().getReminders()
        assertThat(retrievedReminder.isNotEmpty(), `is`(true))
    }

    @Test
    fun getReminderById() = runBlockingTest {
        val newReminder = ReminderDTO("Title Test", "Description Test", "Location Test", 0.0, 0.0)
        db.reminderDao().saveReminder(newReminder)

        val retrievedReminder = db.reminderDao().getReminderById(newReminder.id)

        assertThat(retrievedReminder as ReminderDTO, notNullValue())
        assertThat(retrievedReminder.id, `is`(newReminder.id))
        assertThat(retrievedReminder.title, `is`(newReminder.title))
        assertThat(retrievedReminder.description, `is`(newReminder.description))
        assertThat(retrievedReminder.location, `is`(newReminder.location))
        assertThat(retrievedReminder.latitude, `is`(newReminder.latitude))
        assertThat(retrievedReminder.longitude, `is`(newReminder.longitude))
    }

    @Test
    fun deleteAllReminders() = runBlockingTest {
        db.reminderDao().saveReminder(ReminderDTO("Title Test", "Description Test", "Location Test", 0.0, 0.0))

        db.reminderDao().deleteAllReminders()
        val emptyReminder = db.reminderDao().getReminders()

        assertThat(emptyReminder.isEmpty(), `is`(true))
    }
}