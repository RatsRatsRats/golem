package golem.matrix.mtj

import golem.*
import golem.matrix.*
import golem.matrix.mtj.backend.*
import no.uib.cipr.matrix.DenseMatrix
import no.uib.cipr.matrix.Matrices
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintStream
import java.text.DecimalFormat
import java.util.*

/**
 * An implementation of the Matrix<Double> interface using MTJ.
 * You should rarely use this class directly, instead use one of the
 * top-level functions in creators.kt (e.g. zeros(5,5)).
 */
class MTJMatrix(var storage: DenseMatrix) : Matrix<Double> {
    override fun getBaseMatrix() = this.storage

    override fun getDoubleData() = this.T.storage.data

    // TODO: Fix UnsupportedOperationException

    override fun diag(): MTJMatrix {
        return MTJMatrix(this.storage.diag())
    }

    override fun max() = storage.maxBy { it.get() }!!.get()
    override fun mean() = elementSum() / (numCols() * numRows())
    override fun min() = storage.minBy { it.get() }!!.get()
    override fun argMax(): Int {
        var max = 0
        for (i in 0..this.numCols() * this.numRows() - 1)
            if (this[i] > this[max])
                max = i
        return max
    }

    override fun argMin(): Int {
        var max = 0
        for (i in 0..this.numCols() * this.numRows() - 1)
            if (this[i] < this[max])
                max = i
        return max
    }

    override fun norm() = normF()
    override fun normF() = this.storage.norm(no.uib.cipr.matrix.Matrix.Norm.Frobenius)
    override fun normIndP1() = this.storage.norm(no.uib.cipr.matrix.Matrix.Norm.One)

    override fun getDouble(i: Int, j: Int) = this.storage.get(i, j)
    override fun getDouble(i: Int) = this.storage[i]
    override fun setDouble(i: Int, v: Double) = this.storage.set(i, v)
    override fun setDouble(i: Int, j: Int, v: Double) = this.storage.set(i, j, v)

    override fun numRows() = this.storage.numRows()
    override fun numCols() = this.storage.numColumns()
    override fun times(other: Matrix<Double>) = MTJMatrix(this.storage.times(castOrBail(other).storage))
    override fun times(other: Double) = MTJMatrix(this.storage.times(other))
    override fun elementTimes(other: Matrix<Double>) = MTJMatrix(this.storage.mod(castOrBail(other).storage))
    override fun mod(other: Matrix<Double>) = elementTimes(other)
    override fun unaryMinus() = MTJMatrix(this.storage.unaryMinus())
    override fun minus(other: Double) = MTJMatrix(this.storage.minusElement(other))
    override fun minus(other: Matrix<Double>) = MTJMatrix(this.storage.minus(castOrBail(other).storage))
    override fun div(other: Int) = MTJMatrix(this.storage.div(other))
    override fun div(other: Double) = MTJMatrix(this.storage.div(other))
    override fun transpose(): MTJMatrix {
        var out = DenseMatrix(this.numCols(), numRows())
        return MTJMatrix(DenseMatrix(this.storage.transpose(out)))
    }

    override fun copy() = MTJMatrix(this.storage.copy())
    override fun set(i: Int, v: Double): Unit = this.storage.set(i, v)
    override fun set(i: Int, j: Int, v: Double) = this.storage.set(i, j, v)
    override fun get(i: Int, j: Int) = this.storage.get(i, j)
    override fun get(i: Int) = this.storage[i]
    override fun getRow(row: Int): MTJMatrix {
        var out = DenseMatrix(1, this.numCols())
        for (col in 0 until this.numCols())
            out.set(0, col, this[row, col])
        return MTJMatrix(out)
    }

    override fun getCol(col: Int) = MTJMatrix(DenseMatrix(Matrices.getColumn(this.storage, col)))
    override fun plus(other: Matrix<Double>) = MTJMatrix(this.storage.plusMatrix(castOrBail(other).storage))
    override fun plus(other: Double) = MTJMatrix(this.storage.plusElement(other))
    override fun chol() = MTJMatrix(DenseMatrix(this.storage.chol()))
    override fun inv() = MTJMatrix(this.storage.inv())
    override fun pinv() = throw UnsupportedOperationException()//= EJMLMatrix(this.storage.pseudoInverse())
    override fun elementSum() = storage.sumByDouble { it.get() }
    override fun trace() = throw UnsupportedOperationException() //= this.storage.trace()
    override fun epow(other: Double) = MTJMatrix(this.storage.powElement(other))
    override fun epow(other: Int) = MTJMatrix(this.storage.powElement(other))
    override fun det(): Double {
        return this.storage.det()
    }

    override fun pow(exponent: Int): MTJMatrix {
        var out = this.copy()
        for (i in 1..exponent - 1)
            out *= this
        return out
    }

    override fun setCol(index: Int, col: Matrix<Double>) {
        for (i in 0..col.numRows() - 1)
            this[i, index] = col[i]
    }

    override fun setRow(index: Int, row: Matrix<Double>) {
        for (i in 0..row.numCols() - 1)
            this[index, i] = row[i]
    }

    override fun getFactory() = golem.matrix.mtj.backend.factoryInstance

    override fun T() = this.T

    override val T: MTJMatrix
        get() = this.transpose()

    override fun solve(A: Matrix<Double>, B: Matrix<Double>): MTJMatrix {
        var out = this.getFactory().zeros(A.numCols(), 1)
        castOrBail(A).storage.solve(castOrBail(B).storage, out.storage)
        return out
    }

    override fun expm(): MTJMatrix {
        // Casts are safe since generation happens from mat.getFactory()
        return golem.matrix.common.expm(this) as MTJMatrix
    }

    override fun LU(): Triple<MTJMatrix, MTJMatrix, MTJMatrix> {
        val (p, L, U) = this.storage.LU()
        return Triple(MTJMatrix(p), MTJMatrix(L), MTJMatrix(U))
    }

    override fun QR(): Pair<MTJMatrix, MTJMatrix> {
        val (Q, R) = this.storage.QR()
        return Pair(MTJMatrix(Q), MTJMatrix(R))
    }

    override fun iterator(): Iterator<Double> {
        class MTJIterator(var matrix: MTJMatrix) : Iterator<Double> {
            private var cursor = 0
            override fun next(): Double {
                cursor += 1
                return matrix[cursor - 1]
            }

            override fun hasNext() = cursor < matrix.numCols() * matrix.numRows()
        }
        return MTJIterator(this)
    }

    override fun toString() = this.repr()


    // TODO: Fix this
    /**
     * Eventually we will support operations between matrices with different
     * backends. However, for now we'll exception out of it.
     *
     */
    private fun castOrBail(mat: Matrix<Double>): MTJMatrix {
        when (mat) {
            is MTJMatrix -> return mat
            else -> {
                val base = mat.getBaseMatrix()
                if (base is DenseMatrix)
                    return MTJMatrix(base)
                else
                // No friendly backend, need to convert manually
                    throw Exception("Operations between matrices with different backends not yet supported.")

            }
        }
    }

    /* These methods are defined in order to support fast non-generic calls. However,
       since our type is Double we'll disable them here in case someone accidentally
       uses them.
     */
    override fun getInt(i: Int, j: Int): Int {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun getFloat(i: Int, j: Int): Float {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun getInt(i: Int): Int {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun getFloat(i: Int): Float {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setInt(i: Int, v: Int) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setFloat(i: Int, v: Float) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setInt(i: Int, j: Int, v: Int) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Int disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    override fun setFloat(i: Int, j: Int, v: Float) {
        throw UnsupportedOperationException("Implicit cast of Double matrix to Float disabled to prevent subtle bugs. " +
                                            "Please call getDouble and cast manually if this is intentional.")
    }

    private fun writeObject(out: ObjectOutputStream) = serializeObject(out)

    private fun readObject(oin: ObjectInputStream) {
        val rows = oin.readObject() as Int
        val cols = oin.readObject() as Int
        this.storage = DenseMatrix(rows, cols)
        this.forEachIndexed { i, d -> this[i] = oin.readObject() as Double }
    }

    private fun readObjectNoData() = deserializeObjectNoData()

}
