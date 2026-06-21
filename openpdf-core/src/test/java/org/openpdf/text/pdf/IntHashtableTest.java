package org.openpdf.text.pdf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IntHashtableTest {

    private IntHashtable table;

    @BeforeEach
    void setUp() {
        table = new IntHashtable();
    }

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------
    @Nested
    class Construction {

        @Test
        void defaultConstructorCreatesEmptyTable() {
            assertTrue(table.isEmpty());
            assertEquals(0, table.size());
        }

        @Test
        void customCapacityConstructorCreatesEmptyTable() {
            IntHashtable t = new IntHashtable(50);
            assertTrue(t.isEmpty());
        }

        @Test
        void customCapacityAndLoadFactorConstructorCreatesEmptyTable() {
            IntHashtable t = new IntHashtable(50, 0.5f);
            assertTrue(t.isEmpty());
        }

        @Test
        void zeroInitialCapacityIsAllowed() {
            // capacity 0 is normalised to 1 internally
            assertDoesNotThrow(() -> new IntHashtable(0));
        }

        @Test
        void negativeCapacityThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> new IntHashtable(-1));
        }

        @Test
        void nonPositiveLoadFactorThrowsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> new IntHashtable(10, 0f));
            assertThrows(IllegalArgumentException.class, () -> new IntHashtable(10, -1f));
        }
    }

    // -------------------------------------------------------------------------
    // put / get
    // -------------------------------------------------------------------------
    @Nested
    class PutAndGet {

        @Test
        void putAndGetSingleEntry() {
            table.put(1, 100);
            assertEquals(100, table.get(1));
        }

        @Test
        void getMissingKeyReturnsZero() {
            assertEquals(0, table.get(999));
        }

        @Test
        void putReturnsZeroForNewKey() {
            assertEquals(0, table.put(42, 7));
        }

        @Test
        void putReturnsPreviousValueOnUpdate() {
            table.put(1, 10);
            long previous = table.put(1, 20);
            assertEquals(10, previous);
        }

        @Test
        void updateDoesNotChangeSizeOrCount() {
            table.put(1, 10);
            table.put(1, 20);
            assertEquals(1, table.size());
            assertEquals(20, table.get(1));
        }

        @Test
        void putNegativeKey() {
            table.put(-5, 55);
            assertEquals(55, table.get(-5));
        }

        @Test
        void putZeroKey() {
            table.put(0, 999);
            assertEquals(999, table.get(0));
        }

        @Test
        void putLargeKey() {
            table.put(Integer.MAX_VALUE, 1);
            assertEquals(1, table.get(Integer.MAX_VALUE));
        }

        @Test
        void putMinLongKey() {
            table.put(Integer.MIN_VALUE, 2);
            assertEquals(2, table.get(Integer.MIN_VALUE));
        }

        @Test
        void multipleDistinctKeys() {
            for (int i = 0; i < 20; i++) {
                table.put(i, i * 10);
            }
            for (int i = 0; i < 20; i++) {
                assertEquals(i * 10, table.get(i));
            }
            assertEquals(20, table.size());
        }
    }

    // -------------------------------------------------------------------------
    // containsKey / contains / containsValue
    // -------------------------------------------------------------------------
    @Nested
    class Contains {

        @Test
        void containsKeyReturnsTrueForExistingKey() {
            table.put(7, 0);
            assertTrue(table.containsKey(7));
        }

        @Test
        void containsKeyReturnsFalseForAbsentKey() {
            assertFalse(table.containsKey(7));
        }

        @Test
        void containsReturnsTrueForExistingValue() {
            table.put(1, 42);
            assertTrue(table.contains(42));
        }

        @Test
        void containsReturnsFalseForAbsentValue() {
            table.put(1, 42);
            assertFalse(table.contains(99));
        }

        @Test
        void containsValueDelegatesToContains() {
            table.put(1, 42);
            assertTrue(table.containsValue(42));
            assertFalse(table.containsValue(0));
        }
    }

    // -------------------------------------------------------------------------
    // remove
    // -------------------------------------------------------------------------
    @Nested
    class Remove {

        @Test
        void removePresentKeyReturnsOldValue() {
            table.put(3, 33);
            assertEquals(33, table.remove(3));
        }

        @Test
        void removePresentKeyDecreasesSize() {
            table.put(3, 33);
            table.remove(3);
            assertEquals(0, table.size());
            assertFalse(table.containsKey(3));
        }

        @Test
        void removeAbsentKeyReturnsZero() {
            assertEquals(0, table.remove(999));
        }

        @Test
        void removeDoesNothingToOtherKeys() {
            table.put(1, 11);
            table.put(2, 22);
            table.remove(1);
            assertEquals(22, table.get(2));
            assertEquals(1, table.size());
        }

        @Test
        void removeHeadOfChainWorks() {
            // Force two keys into the same bucket by inserting many entries
            for (int i = 0; i < 50; i++) {
                table.put(i, i);
            }
            table.remove(0);
            assertFalse(table.containsKey(0));
            assertEquals(49, table.size());
        }
    }

    // -------------------------------------------------------------------------
    // clear
    // -------------------------------------------------------------------------
    @Nested
    class Clear {

        @Test
        void clearEmptiesTable() {
            table.put(1, 1);
            table.put(2, 2);
            table.clear();
            assertTrue(table.isEmpty());
            assertEquals(0, table.size());
        }

        @Test
        void afterClearGetReturnsZero() {
            table.put(1, 100);
            table.clear();
            assertEquals(0, table.get(1));
        }

        @Test
        void canInsertAfterClear() {
            table.put(1, 1);
            table.clear();
            table.put(2, 20);
            assertEquals(20, table.get(2));
            assertEquals(1, table.size());
        }
    }

    // -------------------------------------------------------------------------
    // rehash (triggered implicitly)
    // -------------------------------------------------------------------------
    @Nested
    class Rehash {

        @Test
        void tableRemainsCorrectAfterRehash() {
            // Default capacity 150; inserting 200 entries will trigger rehash
            for (int i = 0; i < 200; i++) {
                table.put(i, i * 3);
            }
            assertEquals(200, table.size());
            for (int i = 0; i < 200; i++) {
                assertEquals(i * 3, table.get(i));
            }
        }
    }

    // -------------------------------------------------------------------------
    // getKeys / toOrderedKeys / getOneKey
    // -------------------------------------------------------------------------
    @Nested
    class KeyRetrieval {

        @Test
        void getKeysReturnsAllKeys() {
            table.put(10, 1);
            table.put(20, 2);
            table.put(30, 3);

            int[] keys = table.getKeys();
            assertEquals(3, keys.length);
            assertArrayContains(keys, 10, 20, 30);
        }

        @Test
        void toOrderedKeysReturnsSortedKeys() {
            table.put(30, 1);
            table.put(10, 2);
            table.put(20, 3);

            int[] keys = table.toOrderedKeys();
            assertArrayEquals(new int[]{10, 20, 30}, keys);
        }

        @Test
        void getOneKeyReturnsZeroForEmptyTable() {
            assertEquals(0, table.getOneKey());
        }

        @Test
        void getOneKeyReturnsAKey() {
            table.put(7, 70);
            table.put(8, 80);
            int key = table.getOneKey();
            assertTrue(key == 7 || key == 8);
        }

        // Helper
        private void assertArrayContains(int[] array, int... expected) {
            for (int exp : expected) {
                boolean found = false;
                for (int val : array) {
                    if (val == exp) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "Expected key " + exp + " not found in array");
            }
        }
    }

    // -------------------------------------------------------------------------
    // clone
    // -------------------------------------------------------------------------
    @Nested
    class CloneTests {

        @Test
        void cloneProducesIndependentCopy() {
            table.put(1, 10);
            table.put(2, 20);

            IntHashtable clone = (IntHashtable) table.clone();

            assertEquals(10, clone.get(1));
            assertEquals(20, clone.get(2));
            assertEquals(table.size(), clone.size());
        }

        @Test
        void mutatingCloneDoesNotAffectOriginal() {
            table.put(1, 10);
            IntHashtable clone = (IntHashtable) table.clone();

            clone.put(1, 999);

            assertEquals(10, table.get(1));
            assertEquals(999, clone.get(1));
        }

        @Test
        void mutatingOriginalDoesNotAffectClone() {
            table.put(1, 10);
            IntHashtable clone = (IntHashtable) table.clone();

            table.put(1, 999);

            assertEquals(10, clone.get(1));
        }
    }

    // -------------------------------------------------------------------------
    // Iterator
    // -------------------------------------------------------------------------
    @Nested
    class IteratorTests {

        @Test
        void iteratorVisitsAllEntries() {
            table.put(1, 11);
            table.put(2, 22);
            table.put(3, 33);

            int count = 0;
            long keySum = 0;
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            while (it.hasNext()) {
                IntHashtable.Entry e = it.next();
                keySum += e.getKey();
                count++;
            }

            assertEquals(3, count);
            assertEquals(6, keySum);
        }

        @Test
        void iteratorOnEmptyTableHasNoNext() {
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            assertFalse(it.hasNext());
        }

        @Test
        void iteratorNextOnEmptyTableThrows() {
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            assertThrows(NoSuchElementException.class, it::next);
        }

        @Test
        void iteratorRemoveThrowsUnsupportedOperationException() {
            table.put(1, 10);
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

        @Test
        void hasNextIsIdempotent() {
            table.put(1, 10);
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            assertTrue(it.hasNext());
            assertTrue(it.hasNext()); // calling twice should not advance
            it.next();
            assertFalse(it.hasNext());
        }
    }

    // -------------------------------------------------------------------------
    // Entry inner class
    // -------------------------------------------------------------------------
    @Nested
    class EntryTests {

        @Test
        void entryGetKeyAndGetValue() {
            table.put(5, 50);
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            IntHashtable.Entry e = it.next();
            assertEquals(5, e.getKey());
            assertEquals(50, e.getValue());
        }

        @Test
        void entryCloneIsDeepCopy() {
            table.put(1, 10);
            Iterator<IntHashtable.Entry> it = table.getEntryIterator();
            IntHashtable.Entry original = it.next();
            IntHashtable.Entry cloned = (IntHashtable.Entry) original.clone();

            assertEquals(original.getKey(), cloned.getKey());
            assertEquals(original.getValue(), cloned.getValue());
            assertNotSame(original, cloned);
        }

    }

}