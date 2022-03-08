package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers
import org.junit.*
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var db: RemindersDatabase
    private lateinit var dao: RemindersDao
    private lateinit var localRepository: RemindersLocalRepository

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initialise() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        dao = db.reminderDao()
        localRepository = RemindersLocalRepository(
            dao,
            Dispatchers.Main
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveReminder_then_getReminderById() = runBlocking {
        val newReminder = ReminderDTO(
            "Title Test",
            "Description Test",
            "Location Test",
            0.0,
            0.0
        )
        localRepository.saveReminder(newReminder)

        val retrievedReminder = localRepository.getReminder(newReminder.id) as Result.Success<ReminderDTO>
        val reminder = retrievedReminder.data
        assertNotNull(reminder)
        assertThat(reminder.id, `is`(newReminder.id))
        assertThat(reminder.title, `is`(newReminder.title))
        assertThat(reminder.description, `is`(newReminder.description))
        assertThat(reminder.location, `is`(newReminder.location))
        assertThat(reminder.latitude, `is`(newReminder.latitude))
        assertThat(reminder.longitude, `is`(newReminder.longitude))
    }

    @Test
    fun getAllReminders() = runBlocking {
        val newReminder1 = ReminderDTO(
            "Title Test1",
            "Description Test1",
            "Location Test1",
            1.1,
            1.1
        )
        val newReminder2 = ReminderDTO(
            "Title Test2",
            "Description Test2",
            "Location Test2",
            2.2,
            2.2
        )
        localRepository.saveReminder(newReminder1)
        localRepository.saveReminder(newReminder2)

        val reminders = localRepository.getReminders() as Result.Success<List<ReminderDTO>>
        val reminderData = reminders.data
        assertThat(reminderData.size, `is`(2))
    }

    @Test
    fun deleteAllReminders() = runBlocking {
        val newReminder = ReminderDTO(
            "Title Test",
            "Description Test",
            "Location Test",
            0.0,
            0.0
        )
        localRepository.saveReminder(newReminder)
        localRepository.deleteAllReminders()

        val result = localRepository.getReminders() as Result.Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun getReminderById_error() = runBlocking {
        val newReminder = ReminderDTO(
            "Title Test",
            "Description Test",
            "Location Test",
            0.0,
            0.0
        )
        localRepository.saveReminder(newReminder)
        localRepository.deleteAllReminders()

        val retrievedReminder = localRepository.getReminder(newReminder.id) as Result.Error
        assertThat(retrievedReminder.message, Matchers.notNullValue())
    }

}