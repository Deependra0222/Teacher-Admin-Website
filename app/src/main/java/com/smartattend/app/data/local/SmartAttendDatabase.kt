package com.smartattend.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartattend.app.data.local.entity.AttendanceHistoryEntity
import com.smartattend.app.data.local.entity.CorrectionRequestEntity
import com.smartattend.app.data.local.entity.MetadataEntity
import com.smartattend.app.data.local.entity.NoticeEntity
import com.smartattend.app.data.local.entity.PendingOperationEntity
import com.smartattend.app.data.local.entity.RegisteredDeviceEntity
import com.smartattend.app.data.local.entity.ScheduleEntity
import com.smartattend.app.data.local.entity.StudentEntity
import com.smartattend.app.data.local.entity.SubjectEntity

@Database(
    entities = [
        StudentEntity::class,
        SubjectEntity::class,
        AttendanceHistoryEntity::class,
        ScheduleEntity::class,
        NoticeEntity::class,
        CorrectionRequestEntity::class,
        RegisteredDeviceEntity::class,
        PendingOperationEntity::class,
        MetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SmartAttendDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao

    companion object {
        @Volatile private var instance: SmartAttendDatabase? = null

        fun getInstance(context: Context): SmartAttendDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                SmartAttendDatabase::class.java,
                "smartattend.db",
            ).build().also { instance = it }
        }
    }
}
