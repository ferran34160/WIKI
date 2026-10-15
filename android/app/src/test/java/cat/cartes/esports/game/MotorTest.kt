package cat.cartes.esports.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorTest {

    @Test
    fun `la raresa tria nomes entre les disponibles`() {
        val motor = MotorSobres()
        val rnd = Random(1)
        repeat(1000) {
            val r = motor.triaRaresa(setOf(2, 4), rnd)
            assertTrue(r == 2 || r == 4)
        }
    }

    @Test
    fun `les probabilitats s'acosten a les configurades`() {
        val motor = MotorSobres()
        val rnd = Random(42)
        val n = 100_000
        val comptes = IntArray(6)
        repeat(n) { comptes[motor.triaRaresa(setOf(1, 2, 3, 4, 5), rnd)]++ }
        assertEquals(0.60, comptes[1] / n.toDouble(), 0.01)
        assertEquals(0.25, comptes[2] / n.toDouble(), 0.01)
        assertEquals(0.01, comptes[5] / n.toDouble(), 0.005)
    }

    @Test
    fun `els sobres s'acumulen cada 10 minuts fins al maxim`() {
        val inici = EstatSobres(guardats = 0, ultimTick = 0L)
        assertEquals(0, Sobres.actualitza(inici, Sobres.MS_PER_SOBRE - 1).guardats)
        assertEquals(1, Sobres.actualitza(inici, Sobres.MS_PER_SOBRE).guardats)
        assertEquals(2, Sobres.actualitza(inici, Sobres.MS_PER_SOBRE * 2 + 5).guardats)
        assertEquals(Sobres.MAX_GUARDATS, Sobres.actualitza(inici, Sobres.MS_PER_SOBRE * 99).guardats)
    }

    @Test
    fun `el rellotge no avanca amb els sobres plens i comenca en gastar-ne un`() {
        val ara = 1_000_000L
        val ple = Sobres.actualitza(EstatSobres(Sobres.MAX_GUARDATS, 0L), ara)
        assertEquals(ara, ple.ultimTick)
        val desPres = Sobres.gasta(ple, ara + 5_000)
        assertEquals(Sobres.MAX_GUARDATS - 1, desPres.guardats)
        assertEquals(ara + 5_000, desPres.ultimTick)
    }

    @Test
    fun `fabricar costa mes que destruir`() {
        for (r in Raresa.MIN..Raresa.MAX) {
            assertTrue(Economia.costFabricar(r) > Economia.valorDestruccio(r))
        }
    }

    @Test
    fun `el compte enrere mai surt de rang`() {
        val e = EstatSobres(0, 1000L)
        assertEquals(Sobres.MS_PER_SOBRE, Sobres.msFinsProper(e, 0L)) // rellotge endarrerit
        assertEquals(0L, Sobres.msFinsProper(EstatSobres(Sobres.MAX_GUARDATS, 0L), 5L))
    }
}
