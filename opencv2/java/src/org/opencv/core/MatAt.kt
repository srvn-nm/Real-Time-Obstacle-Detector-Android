package org.opencv.core

import org.opencv.core.Mat.*
import java.lang.RuntimeException

// —————————————————————————————————————————————————————————————————————————————
// 1) Extension functions on Mat for UByteArray ↔ ByteArray conversion
// —————————————————————————————————————————————————————————————————————————————

/** Read signed bytes from Mat into a UByteArray by converting each Byte → UByte. */
@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.get(row: Int, col: Int, data: UByteArray) {
    val tmp = ByteArray(data.size)
    get(row, col, tmp)
    for (i in data.indices) {
        data[i] = tmp[i].toUByte()
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.get(indices: IntArray, data: UByteArray) {
    val tmp = ByteArray(data.size)
    get(indices, tmp)
    for (i in data.indices) {
        data[i] = tmp[i].toUByte()
    }
}

/** Write a UByteArray to Mat by converting each UByte → Byte. */
@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.put(row: Int, col: Int, data: UByteArray) {
    val tmp = ByteArray(data.size)
    for (i in data.indices) {
        tmp[i] = data[i].toByte()
    }
    put(row, col, tmp)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.put(indices: IntArray, data: UByteArray) {
    val tmp = ByteArray(data.size)
    for (i in data.indices) {
        tmp[i] = data[i].toByte()
    }
    put(indices, tmp)
}

// —————————————————————————————————————————————————————————————————————————————
// 2) Extension functions on Mat for UShortArray ↔ ShortArray conversion
// —————————————————————————————————————————————————————————————————————————————

/** Read signed shorts from Mat into a UShortArray by converting each Short → UShort. */
@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.get(row: Int, col: Int, data: UShortArray) {
    val tmp = ShortArray(data.size)
    get(row, col, tmp)
    for (i in data.indices) {
        data[i] = tmp[i].toUShort()
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.get(indices: IntArray, data: UShortArray) {
    val tmp = ShortArray(data.size)
    get(indices, tmp)
    for (i in data.indices) {
        data[i] = tmp[i].toUShort()
    }
}

/** Write a UShortArray to Mat by converting each UShort → Short. */
@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.put(row: Int, col: Int, data: UShortArray) {
    val tmp = ShortArray(data.size)
    for (i in data.indices) {
        tmp[i] = data[i].toShort()
    }
    put(row, col, tmp)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Mat.put(indices: IntArray, data: UShortArray) {
    val tmp = ShortArray(data.size)
    for (i in data.indices) {
        tmp[i] = data[i].toShort()
    }
    put(indices, tmp)
}

// —————————————————————————————————————————————————————————————————————————————
// 3) Tuple classes for multi-channel values
// —————————————————————————————————————————————————————————————————————————————

class Tuple2<T>(val _0: T, val _1: T)
class Tuple3<T>(val _0: T, val _1: T, val _2: T)
class Tuple4<T>(val _0: T, val _1: T, val _2: T, val _3: T)

// Component operators for destructuring
operator fun <T> Tuple2<T>.component1(): T = this._0
operator fun <T> Tuple2<T>.component2(): T = this._1

operator fun <T> Tuple3<T>.component1(): T = this._0
operator fun <T> Tuple3<T>.component2(): T = this._1
operator fun <T> Tuple3<T>.component3(): T = this._2

operator fun <T> Tuple4<T>.component1(): T = this._0
operator fun <T> Tuple4<T>.component2(): T = this._1
operator fun <T> Tuple4<T>.component3(): T = this._2
operator fun <T> Tuple4<T>.component4(): T = this._3

// Convenience factory functions
fun <T> T2(_0: T, _1: T): Tuple2<T> = Tuple2(_0, _1)
fun <T> T3(_0: T, _1: T, _2: T): Tuple3<T> = Tuple3(_0, _1, _2)
fun <T> T4(_0: T, _1: T, _2: T, _3: T): Tuple4<T> = Tuple4(_0, _1, _2, _3)

// —————————————————————————————————————————————————————————————————————————————
// 4) "at" extension to return a typed Atable<T>
// —————————————————————————————————————————————————————————————————————————————

/**
 * Example usage:
 * val (b, g, r) = mat.at<UByte>(50, 50).getV3c()
 * mat.at<UByte>(50, 50).setV3c(Mat.Tuple3(245u, 113u, 34u))
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> Mat.at(row: Int, col: Int): Atable<T> =
    when (T::class) {
        Byte::class, Double::class, Float::class, Int::class, Short::class ->
            at(T::class.java, row, col)
        UByte::class -> AtableUByte(this, row, col) as Atable<T>
        UShort::class -> AtableUShort(this, row, col) as Atable<T>
        else -> throw RuntimeException("Unsupported class: ${T::class.java}")
    }

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Mat.at(indices: IntArray): Atable<T> =
    when (T::class) {
        Byte::class, Double::class, Float::class, Int::class, Short::class ->
            at(T::class.java, indices)
        UByte::class -> AtableUByte(this, indices) as Atable<T>
        UShort::class -> AtableUShort(this, indices) as Atable<T>
        else -> throw RuntimeException("Unsupported class: ${T::class.java}")
    }

// —————————————————————————————————————————————————————————————————————————————
// 5) Atable implementations for UByte and UShort
// —————————————————————————————————————————————————————————————————————————————

/**
 * Atable for UByte Mat access (1–4 channels).
 * Converts between ByteArray (signed) and UByteArray manually.
 */
class AtableUByte(
    private val mat: Mat,
    private val indices: IntArray
) : Atable<UByte> {

    constructor(mat: Mat, row: Int, col: Int) : this(mat, intArrayOf(row, col))

    /** Read one UByte at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV(): UByte {
        val arr = UByteArray(1)
        mat.get(indices, arr)
        return arr[0]
    }

    /** Write one UByte at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV(v: UByte) {
        mat.put(indices, ubyteArrayOf(v))
    }

    /** Read a 2-channel UByte pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV2c(): Mat.Tuple2<UByte> {
        val arr = UByteArray(2)
        mat.get(indices, arr)
        return Mat.Tuple2(arr[0], arr[1])
    }

    /** Write a 2-channel UByte pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV2c(v: Mat.Tuple2<UByte>?) {
        if (v != null) {
            val arr = UByteArray(2).apply {
                this[0] = v._0
                this[1] = v._1
            }
            mat.put(indices, arr)
        }
    }

    /** Read a 3-channel UByte pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV3c(): Mat.Tuple3<UByte> {
        val arr = UByteArray(3)
        mat.get(indices, arr)
        return Mat.Tuple3(arr[0], arr[1], arr[2])
    }

    /** Write a 3-channel UByte pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV3c(v: Mat.Tuple3<UByte>?) {
        if (v != null) {
            val arr = UByteArray(3).apply {
                this[0] = v._0
                this[1] = v._1
                this[2] = v._2
            }
            mat.put(indices, arr)
        }
    }

    /** Read a 4-channel UByte pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV4c(): Mat.Tuple4<UByte> {
        val arr = UByteArray(4)
        mat.get(indices, arr)
        return Mat.Tuple4(arr[0], arr[1], arr[2], arr[3])
    }

    /** Write a 4-channel UByte pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV4c(v: Mat.Tuple4<UByte>?) {
        if (v != null) {
            val arr = UByteArray(4).apply {
                this[0] = v._0
                this[1] = v._1
                this[2] = v._2
                this[3] = v._3
            }
            mat.put(indices, arr)
        }
    }
}

/**
 * Atable for UShort Mat access (1–4 channels).
 * Converts between ShortArray (signed) and UShortArray manually.
 */
class AtableUShort(
    private val mat: Mat,
    private val indices: IntArray
) : Atable<UShort> {

    constructor(mat: Mat, row: Int, col: Int) : this(mat, intArrayOf(row, col))

    /** Read one UShort at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV(): UShort {
        val arr = UShortArray(1)
        mat.get(indices, arr)
        return arr[0]
    }

    /** Write one UShort at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV(v: UShort) {
        mat.put(indices, ushortArrayOf(v))
    }

    /** Read a 2-channel UShort pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV2c(): Mat.Tuple2<UShort> {
        val arr = UShortArray(2)
        mat.get(indices, arr)
        return Mat.Tuple2(arr[0], arr[1])
    }

    /** Write a 2-channel UShort pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV2c(v: Mat.Tuple2<UShort>?) {
        if (v != null) {
            val arr = UShortArray(2).apply {
                this[0] = v._0
                this[1] = v._1
            }
            mat.put(indices, arr)
        }
    }

    /** Read a 3-channel UShort pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV3c(): Mat.Tuple3<UShort> {
        val arr = UShortArray(3)
        mat.get(indices, arr)
        return Mat.Tuple3(arr[0], arr[1], arr[2])
    }

    /** Write a 3-channel UShort pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV3c(v: Mat.Tuple3<UShort>?) {
        if (v != null) {
            val arr = UShortArray(3).apply {
                this[0] = v._0
                this[1] = v._1
                this[2] = v._2
            }
            mat.put(indices, arr)
        }
    }

    /** Read a 4-channel UShort pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun getV4c(): Mat.Tuple4<UShort> {
        val arr = UShortArray(4)
        mat.get(indices, arr)
        return Mat.Tuple4(arr[0], arr[1], arr[2], arr[3])
    }

    /** Write a 4-channel UShort pixel at (indices). */
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun setV4c(v: Mat.Tuple4<UShort>?) {
        if (v != null) {
            val arr = UShortArray(4).apply {
                this[0] = v._0
                this[1] = v._1
                this[2] = v._2
                this[3] = v._3
            }
            mat.put(indices, arr)
        }
    }
}