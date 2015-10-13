package golem

import golem.util.assertMatrixEquals
import org.junit.Assert
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Test each back-end for similar compliance with API
 */
class pylab_tests {
    var facs = arrayOf(golem.matrix.ejml.EJMLMatrixFactory(),
                           golem.matrix.mtj.MTJMatrixFactory())

    @Test
    fun testAllBackends()
    {
        for (f in facs) {
            println("Testing $f\n")
            golem.factory = f
            testSin()
            testRandn()
            testEPow()
            testPow()
        }

    }

    fun testSin()
    {
        var a = zeros(2,2)
        a[0,0] = PI/2
        a[0,1] = 3.0
        a[1,0] = -PI/2

        var expected = mat[ 1.0, 3.0 end
                           -1.0, 0.0]

        assertMatrixEquals(sin(a), expected)

    }

    fun testRandn()
    {
        var a = 2*randn(1,1000000)

        Assert.assertEquals(mean(a), 0.0, .01)
    }

    fun testEPow()
    {
        var a = mat[1,2 end
                    3,4]
        a = a epow 3

        var aE = mat[ 1 pow 3,  2 pow 3 end
                     (3 pow 3), 4 pow 3]

        assertMatrixEquals(a, aE)
    }

    fun testPow()
    {
        var a = mat[2,0,0 end
                    0,1,0 end
                    0,0,4]

        var a3 = a pow 3

        assertMatrixEquals(a3, a*a*a)
    }
}
