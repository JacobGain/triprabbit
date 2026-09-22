package gain.jacob.roadjournal.core.usecase

import gain.jacob.roadjournal.core.model.DistanceUnit
import gain.jacob.roadjournal.core.model.Vehicle
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ResolveSelectedVehicleTest {
    private fun vehicle(id: Long, archived: Boolean = false) = Vehicle(id,"Vehicle $id",null,null,null,null,DistanceUnit.KILOMETERS,null,null,Instant.EPOCH,if(archived) Instant.EPOCH else null)
    @Test fun `selected vehicle restored`() = assertEquals(2L, resolveSelectedVehicle(listOf(vehicle(1),vehicle(2)),2))
    @Test fun `missing selected vehicle falls back`() = assertEquals(1L, resolveSelectedVehicle(listOf(vehicle(1)),99))
    @Test fun `archived selected vehicle falls back when active list excludes it`() = assertEquals(1L, resolveSelectedVehicle(listOf(vehicle(1)),2))
    @Test fun `deleted selected vehicle falls back`() = assertEquals(3L, resolveSelectedVehicle(listOf(vehicle(3)),2))
    @Test fun `no active vehicle produces no selection`() = assertNull(resolveSelectedVehicle(emptyList(),2))
}
