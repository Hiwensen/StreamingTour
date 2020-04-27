package com.shaowei.streaming.audio

import org.junit.After
import org.junit.Before
import org.junit.Test

class AudioActivityTest {
    val TAG = AudioActivityTest::class.java.simpleName

    @Before
    fun setUp() {
    }

    @Test
    fun testFinally() {
        try {
            val a = 1
            val b = 0
            val c = a/b
        } catch (e: Exception) {
            println("catch exception:$e")
        } finally {
            println("finally")
        }
    }

    @After
    fun tearDown() {
    }
}