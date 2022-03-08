package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.junit.Assert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {


    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var appContext: Context

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun initialise() {
        appContext = ApplicationProvider.getApplicationContext()
        dataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()

        viewModel.saveReminder(
            ReminderDataItem(
                "Test Reminder",
                "Hi, this is a test description",
                "ApplePark",
                37.33486823649444,
                -122.00893468243808
            )
        )
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))

        assertThat(viewModel.showToast.getOrAwaitValue(),`is`(appContext.getString(R.string.reminder_saved)))
        assertEquals(viewModel.navigationCommand.getOrAwaitValue(), NavigationCommand.Back)
    }

    @Test
    fun onClear_check_all_set_to_null() {
        viewModel.onClear()
        assertNull(viewModel.reminderTitle.getOrAwaitValue())
        assertNull(viewModel.reminderDescription.getOrAwaitValue())
        assertNull(viewModel.reminderSelectedLocationStr.getOrAwaitValue())
        assertNull(viewModel.selectedPOI.getOrAwaitValue())
        assertNull(viewModel.latitude.getOrAwaitValue())
        assertNull(viewModel.longitude.getOrAwaitValue())
    }

    @Test
    fun getReminderDataItem_notEmpty(){
        viewModel.reminderTitle.value = "Test Reminder"
        viewModel.reminderDescription.value = "Hi, this is a test description"
        viewModel.reminderSelectedLocationStr.value = "ApplePark"
        viewModel.latitude.value = 37.33486823649444
        viewModel.longitude.value = -122.00893468243808
        viewModel.selectedPOI.value = null

        val reminderDataItem = viewModel.getReminderDataItem()
        assertThat(reminderDataItem.description, `is`("Hi, this is a test description"))
        assertThat(reminderDataItem.title, `is`("Test Reminder"))
        assertThat(reminderDataItem.location, `is`("ApplePark"))
        assertEquals(reminderDataItem.latitude, 37.33486823649444)
        assertEquals(reminderDataItem.longitude, -122.00893468243808)
        assertNull(viewModel.selectedPOI.getOrAwaitValue())
    }


    @Test
    fun validateAndSaveReminder_snackbar_errors() {
        dataSource.setReturnError(true)

        viewModel.validateAndSaveReminder(ReminderDataItem(null, "Test Description", "Test Location", 0.0, 0.0))
        assertEquals(viewModel.showSnackBarInt.getOrAwaitValue(), R.string.err_enter_title)

        viewModel.validateAndSaveReminder(ReminderDataItem("Test Title", "Test Description", null, 0.0, 0.0))
        assertEquals(viewModel.showSnackBarInt.getOrAwaitValue(),R.string.err_select_location)
    }

}