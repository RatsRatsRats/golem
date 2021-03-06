package golem

import golem.util.test.*
import org.junit.Test

//@formatter:off

class ExtensionsTests {

    @Test
    fun testCumSum() {
        allBackends {
            var a = mat[1, 2, 3, 4, 1]
            var expected = mat[1, 3, 6, 10, 11]
            assertMatrixEquals(expected, cumsum(a))

            a = mat[1, 2 end
                    3, 4]
            expected = mat[1, 3, 6, 10]
            assertMatrixEquals(expected, cumsum(a))
        }
    }

    @Test
    fun testFill() {
        allBackends {
            var a = zeros(2, 2)
            a.fill { i, j -> i + j * 1.0 }
            var expected = mat[0, 1 end
                               1, 2]

            assertMatrixEquals(expected, a)
        }
    }

    @Test
    fun testEachRow() {
        allBackends {
            var a = mat[1, 0, 3 end
                        5, 1, 6]

            var out = arrayOf(0, 0)

            a.eachRow { out[it[1].toInt()] = it[0].toInt() }

            assert(out[0] == 1)
            assert(out[1] == 5)
        }
    }

    @Test
    fun testEachCol() {
        allBackends {
            var a = mat[1, 0, 2 end
                        -2, 2, 6]

            var out = arrayOf(0, 0, 0)

            a.eachCol { out[it[0].toInt()] = it[1].toInt() }

            assert(out[0] == 2)
            assert(out[1] == -2)
            assert(out[2] == 6)

        }
    }

    @Test
    fun testEach() {
        allBackends {
            var a = mat[1, 0, 2 end
                        -2, 2, 6]

            var out = 0.0

            a.each { out = it + 3.0 }

            assert(out == 9.0)
        }
    }

    @Test
    fun testEachIndexed() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6]
            var out = zeros(2, 3)
            a.eachIndexed { row, col, ele ->
                out[row, col] = ele + 3
            }
            assertMatrixEquals(a + 3, out)
        }
    }

    @Test
    fun testMapElements() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]

            var expected = mat[.5, 1, 1.5 end
                               2, 2.5, 3   end
                               3.5, 4, 4.5]
            assertMatrixEquals(expected, a.mapMat { it / 2 })
        }
    }

    @Test
    fun testMapElementsIndexed() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]

            var expected = mat[0, 0, 0   end
                               0, 2.5, 6   end
                               0, 8, 18]
            var out = a.mapMatIndexed { row, col, ele ->
                ele / 2 * row * col
            }
            assertMatrixEquals(expected, out)
        }
    }

    @Test
    fun testMapRows() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]

            var expected = mat[3, 2, 1, 3 end
                               6, 5, 4, 6 end
                               9, 8, 7, 9]

            assertMatrixEquals(expected, a.mapRows { mat[it[2], it[1], it[0], it[2]] })
        }
    }

    @Test
    fun testMapRowsToList() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6]
            var out = a.mapRowsToList {
                it[0].toString()
            }

            assert(out[1].equals("4.0"))

        }
    }

    @Test
    fun testMapCols() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]

            var expected = mat[7, 8, 9 end
                               4, 5, 6 end
                               1, 2, 3 end
                               7, 8, 9]

            assertMatrixEquals(expected, a.mapCols { mat[it[2], it[1], it[0], it[2]] })
        }
    }

    @Test
    fun testMapColsToList() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6]
            var out = a.mapColsToList {
                it[0].toString()
            }

            assert(out[1].equals("2.0"))

        }
    }

    @Test
    fun testTo2DArray() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6]
            var expected = arrayOf(doubleArrayOf(1.0, 2.0, 3.0),
                                   doubleArrayOf(4.0, 5.0, 6.0))

            var out = a.to2DArray()
            for (row in 0..1)
                for (col in 0..2)
                    assert(out[row][col] == expected[row][col])

            assert(out[1][0] == 4.0)
        }
    }

    @Test
    fun testAny() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]
            assert(a.any { it > 8.5 } == true)
            assert(a.any { it > 7 } == true)
            assert(a.any { it > 9 } == false)
        }
    }

    @Test
    fun testAll() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]
            assert(a.all { it > 0.9 } == true)
            assert(a.all { it > 1.0 } == false)
            assert(a.all { it > 8 } == false)
        }
    }

    @Test
    fun testMapFromIterable() {
        allBackends {
            var a = mat[1, 2, 3 end
                        4, 5, 6 end
                        7, 8, 9]
            var out = a.map { it.toString() }
            assert(out[0].equals("1.0"))
            assert(out[4].equals("5.0"))
        }
    }
}
