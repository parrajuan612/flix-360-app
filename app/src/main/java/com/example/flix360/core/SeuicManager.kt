package com.example.flix360.core

import android.util.Log

/**
 * Reflection-based bridge for Seuic Cruise 2 UHFService.
 * Works without SDK at compile-time. Falls back to unsupported when reflection fails.
 */
object SeuicManager {
    private const val TAG = "SeuicManager"
    private const val CLASS_NAME = "com.seuic.uhf.UHFService"

    @Volatile
    var isSupported: Boolean = false
        private set

    private var clazz: Class<*>? = null
    private var instance: Any? = null

    private var mGetInstance: java.lang.reflect.Method? = null
    private var mOpen: java.lang.reflect.Method? = null
    private var mSetPower: java.lang.reflect.Method? = null
    private var mInventoryStart: java.lang.reflect.Method? = null
    private var mInventoryStop: java.lang.reflect.Method? = null
    private var mGetTagIDs: java.lang.reflect.Method? = null
    private var mClose: java.lang.reflect.Method? = null

    /** Try to load class and methods; safe to call multiple times. */
    fun init(): Boolean {
        // Fast path: if we already have an active instance, don't recreate or re-open
        if (instance != null && isSupported) return true
        return try {
            if (clazz == null) {
                clazz = Class.forName(CLASS_NAME)
                mGetInstance = clazz!!.getMethod("getInstance")
                // Methods may be public instance methods
                mOpen = clazz!!.getMethod("open")
                runCatching { mSetPower = clazz!!.getMethod("setPower", Int::class.javaPrimitiveType) }
                mInventoryStart = clazz!!.getMethod("inventoryStart")
                mInventoryStop = clazz!!.getMethod("inventoryStop")
                // Return type could be List<byte[]> or List<String>
                mGetTagIDs = clazz!!.getMethod("getTagIDs")
                // Close/Release method may exist
                runCatching { mClose = clazz!!.getMethod("close") }
                runCatching { if (mClose == null) mClose = clazz!!.getMethod("release") }
            }
            if (instance == null) {
                instance = mGetInstance!!.invoke(null)
            }
            // Open reader
            runCatching { mOpen?.invoke(instance) }
            isSupported = (clazz != null && instance != null)
            isSupported
        } catch (cnf: ClassNotFoundException) {
            Log.w(TAG, "Seuic UHFService not present: ${cnf.message}")
            isSupported = false
            false
        } catch (t: Throwable) {
            Log.w(TAG, "Seuic reflection init failed: ${t.message}")
            isSupported = false
            false
        }
    }

    fun setPower(power: Int): Boolean {
        if (!isSupported) return false
        return try {
            mSetPower?.invoke(instance, power)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "setPower failed: ${t.message}")
            false
        }
    }

    fun startScanning(): Boolean {
        if (!isSupported) return false
        return try {
            mInventoryStart?.invoke(instance)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "inventoryStart failed: ${t.message}")
            false
        }
    }

    fun stopScanning(): Boolean {
        if (!isSupported) return false
        return try {
            mInventoryStop?.invoke(instance)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "inventoryStop failed: ${t.message}")
            false
        }
    }

    /**
     * Returns EPCs as upper-case hex Strings.
     */
    @Suppress("UNCHECKED_CAST")
    fun getTags(): List<String> {
        if (!isSupported) return emptyList()
        return try {
            val anyList = mGetTagIDs?.invoke(instance)
            when (anyList) {
                is List<*> -> {
                    val out = mutableListOf<String>()
                    anyList.forEach { e ->
                        when (e) {
                            is String -> out += e
                            is ByteArray -> out += e.joinToString("") { b -> "%02X".format(b) }
                            else -> out += e?.toString() ?: ""
                        }
                    }
                    out
                }
                else -> emptyList()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "getTags failed: ${t.message}")
            emptyList()
        }
    }

    fun close(): Boolean {
        return try {
            mClose?.invoke(instance)
            isSupported = false
            instance = null
            true
        } catch (t: Throwable) {
            Log.w(TAG, "close failed: ${t.message}")
            false
        }
    }
}
