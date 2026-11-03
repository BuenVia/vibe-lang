package com.vibelang.android.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit description of the Vibe Lang REST API. Paths are relative to BuildConfig.API_BASE_URL. */
interface VibeLangApi {
    // Courses
    @GET("api/courses")
    suspend fun courses(): List<Course>

    @POST("api/courses")
    suspend fun createCourse(@Body body: CourseRequest): Course

    @PUT("api/courses/{id}")
    suspend fun updateCourse(@Path("id") id: Long, @Body body: CourseRequest): Course

    @DELETE("api/courses/{id}")
    suspend fun deleteCourse(@Path("id") id: Long): Response<Unit>

    // Categories
    @GET("api/courses/{courseId}/categories")
    suspend fun categories(@Path("courseId") courseId: Long): List<Category>

    @POST("api/courses/{courseId}/categories")
    suspend fun createCategory(@Path("courseId") courseId: Long, @Body body: CategoryRequest): Category

    @PUT("api/courses/{courseId}/categories/order")
    suspend fun reorderCategories(@Path("courseId") courseId: Long, @Body body: ReorderRequest): List<Category>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryRequest): Category

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // Subjects
    @GET("api/courses/{courseId}/subjects")
    suspend fun subjects(@Path("courseId") courseId: Long): List<Subject>

    @PUT("api/courses/{courseId}/subjects/order")
    suspend fun reorderSubjects(@Path("courseId") courseId: Long, @Body body: ReorderRequest): List<Subject>

    @GET("api/subjects/{id}")
    suspend fun subject(@Path("id") id: Long): Subject

    @POST("api/subjects")
    suspend fun createSubject(@Body body: SubjectRequest): Subject

    @PUT("api/subjects/{id}")
    suspend fun updateSubject(@Path("id") id: Long, @Body body: SubjectRequest): Subject

    @DELETE("api/subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: Long): Response<Unit>

    // Lesson (Response<> so a 404 "no lesson yet" doesn't throw)
    @GET("api/subjects/{id}/lesson")
    suspend fun lesson(@Path("id") subjectId: Long): Response<Lesson>

    @PUT("api/subjects/{id}/lesson")
    suspend fun saveLesson(@Path("id") subjectId: Long, @Body body: LessonRequest): Lesson

    @DELETE("api/subjects/{id}/lesson")
    suspend fun deleteLesson(@Path("id") subjectId: Long): Response<Unit>

    // Vocabulary
    @GET("api/subjects/{id}/vocabulary")
    suspend fun vocabulary(@Path("id") subjectId: Long): List<Vocabulary>

    @POST("api/subjects/{id}/vocabulary")
    suspend fun addVocabulary(@Path("id") subjectId: Long, @Body body: VocabularyRequest): Vocabulary

    @POST("api/subjects/{id}/vocabulary/bulk")
    suspend fun addVocabularyBulk(@Path("id") subjectId: Long, @Body body: BulkVocabularyRequest): List<Vocabulary>

    @PUT("api/vocabulary/{id}")
    suspend fun updateVocabulary(@Path("id") id: Long, @Body body: VocabularyRequest): Vocabulary

    @DELETE("api/vocabulary/{id}")
    suspend fun deleteVocabulary(@Path("id") id: Long): Response<Unit>

    // Sentences (Fill the gap)
    @GET("api/subjects/{id}/sentences")
    suspend fun sentences(@Path("id") subjectId: Long): List<Sentence>

    @POST("api/subjects/{id}/sentences")
    suspend fun addSentence(@Path("id") subjectId: Long, @Body body: SentenceRequest): Sentence

    @POST("api/subjects/{id}/sentences/bulk")
    suspend fun addSentencesBulk(@Path("id") subjectId: Long, @Body body: BulkSentenceRequest): List<Sentence>

    @PUT("api/sentences/{id}")
    suspend fun updateSentence(@Path("id") id: Long, @Body body: SentenceRequest): Sentence

    @DELETE("api/sentences/{id}")
    suspend fun deleteSentence(@Path("id") id: Long): Response<Unit>

    // Spaced repetition
    @POST("api/reviews")
    suspend fun review(@Body body: ReviewRequest): ReviewResult

    @GET("api/courses/{courseId}/due")
    suspend fun due(
        @Path("courseId") courseId: Long,
        @Query("newLimit") newLimit: Int,
        @Query("zone") zone: String,
        @Query("limit") limit: Int = 50,
    ): DueResponse

    @GET("api/courses/{courseId}/progress")
    suspend fun progress(@Path("courseId") courseId: Long, @Query("zone") zone: String): List<SubjectProgress>
}
