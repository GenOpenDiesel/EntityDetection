package de.themoep.entitydetection;

import de.themoep.entitydetection.searcher.SearchType;
import org.bukkit.entity.EntityType;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

public class NewEntitySearchTest {
    @Test
    public void preservesNamesEndingInS() {
        EntityType nautilus = availableType("NAUTILUS");
        assumeNotNull(nautilus);

        assertEquals(nautilus, Utils.findEntityType("nautilus"));
        assertEquals(nautilus, Utils.findEntityType("NAUTILUS"));
        assertEquals(nautilus, Utils.findEntityType("nautiluss"));
    }

    @Test
    public void classifiesNewMobs() {
        EntityType nautilus = availableType("NAUTILUS");
        EntityType zombieNautilus = availableType("ZOMBIE_NAUTILUS");
        EntityType camelHusk = availableType("CAMEL_HUSK");
        EntityType parched = availableType("PARCHED");
        EntityType copperGolem = availableType("COPPER_GOLEM");
        assumeNotNull(nautilus, zombieNautilus, camelHusk, parched, copperGolem);

        assertTrue(Arrays.asList(SearchType.PASSIVE.getEntities()).contains(nautilus));
        assertTrue(Arrays.asList(SearchType.PASSIVE.getEntities()).contains(copperGolem));
        for (EntityType undead : Arrays.asList(zombieNautilus, camelHusk)) {
            assertTrue(Arrays.asList(SearchType.MONSTER.getEntities()).contains(undead));
            assertFalse(Arrays.asList(SearchType.PASSIVE.getEntities()).contains(undead));
        }
        assertTrue(Arrays.asList(SearchType.MONSTER.getEntities()).contains(parched));
    }

    private static EntityType availableType(String name) {
        try {
            return EntityType.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
