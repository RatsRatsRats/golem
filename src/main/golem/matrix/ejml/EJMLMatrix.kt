package golem.matrix.ejml

import golem.*
import golem.matrix.*
import golem.matrix.ejml.backend.*
import org.ejml.data.DenseMatrix64F
import org.ejml.ops.CommonOps
import org.ejml.ops.MatrixIO
import org.ejml.simple.SimpleMatrix
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintStream

/**
 * An implementation of the Matrix<Double> interface using EJML.
 * You should rarely use this class directly, instead use one of the
 * top-level functions in creators.kt (e.g. zeros(5,5)).
 */
class EJMLMatrix(var storage: SimpleMatrix) : Matrix<Double> {
    override fun getBaseMatrix() = this.storage

    override fun getDoubleData() = this.storage.matrix.getData()
    override fun diag() = EJMLMatrix(storage.extractDiag())
    override fun max() = CommonOps.elementMax(this.storage.matrix)
    override fun mean() = elementSum() / (numCols() * numRows())
    override fun min() = CommonOps.elementMin(this.storage.matrix)
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

    override fun getDouble(i: Int, j: Int) = this.storage.get(i, j)
    override fun getDouble(i: Int) = this.storage.get(i)
    override fun setDouble(i: Int, v: Double) = this.storage.set(i, v)
    override fun setDouble(i: Int, j: Int, v: Double) = this.storage.set(i, j, v)

    override fun numRows() = this.storage.numRows()
    override fun numCols() = this.storage.numCols()
    override fun times(other: Matrix<Double>) = EJMLMatrix(this.storage.times(castOrBail(other).storage))
    override fun times(other: Double) = EJMLMatrix(this.storage.times(other))
    override fun elementTimes(other: Matrix<Double>) = EJMLMatrix(this.storage.elementMult(castOrBail(other).storage))
    override fun mod(other: Matrix<Double>) = EJMLMatrix(this.storage.mod(castOrBail(other).storage))
    override fun unaryMinus() = EJMLMatrix(this.storage.unaryMinus())
    override fun minus(other: Double) = EJMLMatrix(this.storage.minus(other))
    override fun minus(other: Matrix<Double>) = EJMLMatrix(this.storage.minus(castOrBail(other).storage))
    override fun div(other: Int) = EJMLMatrix(this.storage.div(other))
    override fun div(other: Double) = EJMLMatrix(this.storage.div(other))
    override fun transpose() = EJMLMatrix(this.storage.transpose())
    override fun copy() = EJMLMatrix(this.storage.copy())
    override fun set(i: Int, v: Double): Unit = this.storage.set(i, v)
    override fun set(i: Int, j: Int, v: Double) = this.storage.set(i, j, v)
    override fun get(i: Int, j: Int) = this.storage.get(i, j)
    override fun get(i: Int) = this.storage.get(i)
    override fun getRow(row: Int) = EJMLMatrix(SimpleMatrix(CommonOps.extractRow(this.storage.matrix, row, null)))
    override fun getCol(col: Int) = EJMLMatrix(SimpleMatrix(CommonOps.extractColumn(this.storage.matrix, col, null)))
    override fun plus(other: Matrix<Double>) = EJMLMatrix(this.storage.plus(castOrBail(other).storage))
    override fun plus(other: Double) = EJMLMatrix(this.storage.plus(other))
    override fun chol(): EJMLMatrix {
        val decomp = this.storage.chol()
        // Copy required to prevent decompose implementations distorting the input matrix
        if (decomp.decompose(this.storage.matrix.copy()))
            return EJMLMatrix(SimpleMatrix(decomp.getT(null)))
        else
            throw Exception("Decomposition failed")
    }
    override fun inv() = EJMLMatrix(this.storage.inv())
    override fun det() = this.storage.determinant()
    override fun pinv() = EJMLMatrix(this.storage.pseudoInverse())
    override fun norm() = normF()
    override fun normF() = this.storage.normF()
    override fun normIndP1() = org.ejml.ops.NormOps.inducedP1(this.storage.matrix)
    override fun elementSum() = this.storage.elementSum()
    override fun trace() = this.storage.trace()
    override fun epow(other: Double) = EJMLMatrix(this.storage.elementPower(other))
    override fun epow(other: Int) = EJMLMatrix(this.storage.elementPower(other.toDouble()))
    override fun pow(exponent: Int): EJMLMatrix {
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

    override fun getFactory() = factoryInstance

    override fun T() = this.T

    override val T: EJMLMatrix
        get() = this.transpose()

    override fun solve(A: Matrix<Double>, B: Matrix<Double>): EJMLMatrix {
        var out = this.getFactory().zeros(A.numCols(), 1)
        CommonOps.solve(castOrBail(A).storage.matrix, castOrBail(B).storage.matrix, out.storage.matrix)
        return out
    }

    override fun expm(): EJMLMatrix {
        // Casts are safe since generation happens from mat.getFactory()
        return golem.matrix.common.expm(this) as EJMLMatrix
    }

    override fun LU(): Triple<EJMLMatrix, EJMLMatrix, EJMLMatrix> {
        val decomp = this.storage.LU()
        return Triple(EJMLMatrix(SimpleMatrix(decomp.getPivot(null))),
                      EJMLMatrix(SimpleMatrix(decomp.getLower(null))),
                      EJMLMatrix(SimpleMatrix(decomp.getUpper(null))))
    }

    override fun QR(): Pair<EJMLMatrix, EJMLMatrix> {
        val decomp = this.storage.QR()
        return Pair(EJMLMatrix(SimpleMatrix(decomp.getQ(null, false))),
                    EJMLMatrix(SimpleMatrix(decomp.getR(null, false))))
    }

    override fun iterator(): Iterator<Double> {
        class MTJIterator(var matrix: EJMLMatrix) : Iterator<Double> {
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
    private fun castOrBail(mat: Matrix<Double>): EJMLMatrix {
        when (mat) {
            is EJMLMatrix -> return mat
            else -> {
                val base = mat.getBaseMatrix()
                if (base is SimpleMatrix)
                    return EJMLMatrix(base)
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
        this.storage = SimpleMatrix(rows, cols)
        this.forEachIndexed { i, d -> this[i] = oin.readObject() as Double }
    }

    private fun readObjectNoData() = deserializeObjectNoData()

}
