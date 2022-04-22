package io.coodoo.workhorse.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.coodoo.workhorse.core.control.StaticConfig;
import io.coodoo.workhorse.core.entity.Job;
import io.coodoo.workhorse.core.entity.JobStatus;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;

public class CollectionListingTest {

    private static Set<Job> DATE_COLLECTION;
    private static int DATE_COLLECTION_EXPONENTIATION;
    private static int DATE_COLLECTION_START_YEAR;

    @BeforeClass
    public static void init() {
        StaticConfig.TIME_ZONE = ZoneId.systemDefault().getId();

        DATE_COLLECTION = new HashSet<>();
        DATE_COLLECTION_EXPONENTIATION = 3;
        DATE_COLLECTION_START_YEAR = 2000;
        long id = 0L;

        for (int year = DATE_COLLECTION_START_YEAR; year < DATE_COLLECTION_START_YEAR + DATE_COLLECTION_EXPONENTIATION; year++) {
            for (int month = 1; month < DATE_COLLECTION_EXPONENTIATION; month++) {
                for (int day = 1; day < DATE_COLLECTION_EXPONENTIATION; day++) {
                    for (int hour = 0; hour < DATE_COLLECTION_EXPONENTIATION; hour++) {
                        for (int minute = 0; minute < DATE_COLLECTION_EXPONENTIATION; minute++) {
                            for (int second = 0; second < DATE_COLLECTION_EXPONENTIATION; second++) {

                                LocalDateTime startOfDay = LocalDateTime.of(year, Month.of(month), day, hour, minute, second).withNano(0);
                                Job jobStartOfDay = new Job();
                                jobStartOfDay.setId(++id);
                                jobStartOfDay.setCreatedAt(startOfDay);
                                DATE_COLLECTION.add(jobStartOfDay);

                                LocalDateTime endOfDay = startOfDay.minus(1l, ChronoUnit.MILLIS);
                                Job jobEndOfDay = new Job();
                                jobEndOfDay.setId(++id);
                                jobEndOfDay.setCreatedAt(endOfDay);
                                DATE_COLLECTION.add(jobEndOfDay);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testGetListingResult() throws Exception {

        int size = 50;
        Set<Job> collection = new HashSet<>();
        for (int i = 0; i < size; ++i) {
            collection.add(new Job());
        }
        ListingParameters listingParameters = new ListingParameters();

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertNotNull(listingResult);

        assertNotNull(listingResult.getResults());
        assertEquals(ListingParameters.DEFAULT_LIMIT, listingResult.getResults().size());

        assertNotNull(listingResult.getMetadata());
        assertNotNull(listingResult.getMetadata().getCount());
        assertEquals(size, listingResult.getMetadata().getCount().intValue());
        assertNotNull(listingResult.getMetadata().getCurrentPage());
        assertEquals(1, listingResult.getMetadata().getCurrentPage().intValue());
        assertNotNull(listingResult.getMetadata().getNumPages());
        assertEquals(3, listingResult.getMetadata().getNumPages().intValue());
    }

    @Test
    public void testGetListingResult_emptyCollection() throws Exception {

        Set<Job> collection = new HashSet<>();
        ListingParameters listingParameters = new ListingParameters();

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(0, listingResult.getResults().size());

        assertEquals(0, listingResult.getMetadata().getCount().intValue());
        assertEquals(1, listingResult.getMetadata().getCurrentPage().intValue());
        assertEquals(1, listingResult.getMetadata().getNumPages().intValue());
    }

    @Test
    public void testGetListingResult_oneResult() throws Exception {

        Set<Job> collection = new HashSet<>();
        collection.add(new Job());
        ListingParameters listingParameters = new ListingParameters();

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());

        assertEquals(1, listingResult.getMetadata().getCount().intValue());
        assertEquals(1, listingResult.getMetadata().getCurrentPage().intValue());
        assertEquals(1, listingResult.getMetadata().getNumPages().intValue());
    }

    @Test
    public void testGetListingResult_exactOnePage() throws Exception {

        int size = ListingParameters.DEFAULT_LIMIT;
        Set<Job> collection = new HashSet<>();
        for (int i = 0; i < size; ++i) {
            collection.add(new Job());
        }
        ListingParameters listingParameters = new ListingParameters();

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(size, listingResult.getResults().size());

        assertEquals(size, listingResult.getMetadata().getCount().intValue());
        assertEquals(1, listingResult.getMetadata().getCurrentPage().intValue());
        assertEquals(1, listingResult.getMetadata().getNumPages().intValue());
    }

    @Test
    public void testGetListingResult_oneMoreThenAPage() throws Exception {

        int size = ListingParameters.DEFAULT_LIMIT + 1;
        Set<Job> collection = new HashSet<>();
        for (int i = 0; i < size; ++i) {
            collection.add(new Job());
        }
        ListingParameters listingParameters = new ListingParameters();

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(20, listingResult.getResults().size());

        assertEquals(size, listingResult.getMetadata().getCount().intValue());
        assertEquals(1, listingResult.getMetadata().getCurrentPage().intValue());
        assertEquals(2, listingResult.getMetadata().getNumPages().intValue());
    }

    @Test
    public void testGetListingResult_oneAtPageTwo() throws Exception {

        int size = ListingParameters.DEFAULT_LIMIT + 1;
        Set<Job> collection = new HashSet<>();
        for (int i = 0; i < size; ++i) {
            collection.add(new Job());
        }
        ListingParameters listingParameters = new ListingParameters();
        listingParameters.setPage(2);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());

        assertEquals(size, listingResult.getMetadata().getCount().intValue());
        assertEquals(2, listingResult.getMetadata().getCurrentPage().intValue());
        assertEquals(2, listingResult.getMetadata().getNumPages().intValue());
    }

    @Test
    public void testGetListingResult_filterNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("NULL");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("null");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName(null);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", CollectionListing.OPERATOR_NULL);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterNotNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("NULL");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("null");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName(null);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", CollectionListing.OPERATOR_NOT_WORD + CollectionListing.OPERATOR_NULL);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterString() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "yyy");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterNotString() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", CollectionListing.OPERATOR_NOT_WORD + "yyy");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterQuotedString() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("xxx");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("xxxx");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "\"xxx\"");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterWildcardString() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("x1y");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("xy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("xyy");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("x12y");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "x" + CollectionListing.WILDCARD_ONE + "y");// x?y

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterStringHasNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName(null);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "yyy");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterStringHasEmpty() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "yyy");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterCaseInsensitivString() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("XXX");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("xXx");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("yYy");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "XXX");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(3, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterSubstring() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("XXX");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("xXx");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("yYy");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "x");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(3, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterLocalDateTime() throws Exception {

        LocalDateTime timestamp1 = WorkhorseUtil.timestamp().minus(3l, ChronoUnit.MINUTES);
        LocalDateTime timestamp2 = timestamp1.plus(1l, ChronoUnit.MILLIS);
        LocalDateTime timestamp3 = timestamp1.plus(2l, ChronoUnit.MILLIS);

        long millis = timestamp2.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toInstant().toEpochMilli();

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(timestamp3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", millis);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterLocalDateTimeInvalid() throws Exception {

        LocalDateTime timestamp1 = WorkhorseUtil.timestamp().minus(3l, ChronoUnit.MINUTES);
        LocalDateTime timestamp2 = timestamp1.plus(1l, ChronoUnit.MILLIS);
        LocalDateTime timestamp3 = timestamp1.plus(2l, ChronoUnit.MILLIS);

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(timestamp3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", "c2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(0, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLocalDateTimeHasNull() throws Exception {

        LocalDateTime timestamp1 = WorkhorseUtil.timestamp().minus(3l, ChronoUnit.MINUTES);
        LocalDateTime timestamp2 = timestamp1.plus(1l, ChronoUnit.MILLIS);

        long millis = timestamp2.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toInstant().toEpochMilli();

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(null);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", millis);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterLocalDateTimeLessThan() throws Exception {

        LocalDateTime timestamp1 = WorkhorseUtil.timestamp().minus(3l, ChronoUnit.MINUTES);
        LocalDateTime timestamp2 = timestamp1.plus(1l, ChronoUnit.MILLIS);
        LocalDateTime timestamp3 = timestamp1.plus(2l, ChronoUnit.MILLIS);

        long millis = timestamp2.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toInstant().toEpochMilli();

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(timestamp3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", CollectionListing.OPERATOR_LT + millis);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
    }

    @Test
    public void testGetListingResult_filterLocalDateTimeGreaterThan() throws Exception {

        LocalDateTime timestamp1 = WorkhorseUtil.timestamp().minus(3l, ChronoUnit.MINUTES);
        LocalDateTime timestamp2 = timestamp1.plus(1l, ChronoUnit.MILLIS);
        LocalDateTime timestamp3 = timestamp1.plus(2l, ChronoUnit.MILLIS);

        long millis = timestamp2.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toInstant().toEpochMilli();

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(timestamp3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", CollectionListing.OPERATOR_GT + millis);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterLocalDateTimeRange() throws Exception {

        LocalDateTime timestamp1 = WorkhorseUtil.timestamp().minus(3l, ChronoUnit.MINUTES);
        LocalDateTime timestamp2 = timestamp1.plus(1l, ChronoUnit.MILLIS);
        LocalDateTime timestamp3 = timestamp1.plus(2l, ChronoUnit.MILLIS);

        long millisFrom = timestamp1.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toInstant().toEpochMilli();
        long millisTo = timestamp2.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toInstant().toEpochMilli();

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(timestamp3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", millisFrom + CollectionListing.OPERATOR_TO + millisTo);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterLocalDateTimeYYYY() throws Exception {

        LocalDateTime timestamp1 = LocalDateTime.of(1999, Month.of(2), 1, 0, 0, 0).withNano(0);
        LocalDateTime timestamp2 = LocalDateTime.of(2000, Month.of(3), 1, 0, 0, 0).withNano(0);
        LocalDateTime timestamp3 = LocalDateTime.of(2001, Month.of(4), 1, 0, 0, 0).withNano(0);

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setCreatedAt(timestamp1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setCreatedAt(timestamp2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setCreatedAt(timestamp3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("createdAt", "2000");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);
        assertEquals(1, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLocalDateTime_DATE_COLLECTION_YYYY() throws Exception {

        ListingParameters listingParameters = new ListingParameters(0); // no limit
        listingParameters.addFilterAttributes("createdAt", "2000");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(DATE_COLLECTION, Job.class, listingParameters);

        for (Job job : listingResult.getResults()) {
            assertEquals(job.toString(), 2000, job.getCreatedAt().getYear());
        }

        assertEquals(DATE_COLLECTION.size() / DATE_COLLECTION_EXPONENTIATION, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLocalDateTime_DATE_COLLECTION_MMYYYY() throws Exception {

        ListingParameters listingParameters = new ListingParameters(0); // no limit
        listingParameters.addFilterAttributes("createdAt", "01.2000");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(DATE_COLLECTION, Job.class, listingParameters);

        for (Job job : listingResult.getResults()) {
            assertEquals(job.toString(), 2000, job.getCreatedAt().getYear());
            assertEquals(job.toString(), 1, job.getCreatedAt().getMonthValue());
        }
        assertEquals(DATE_COLLECTION.size() / DATE_COLLECTION_EXPONENTIATION / 2, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLocalDateTime_DATE_COLLECTION_DDMMYYYY() throws Exception {

        ListingParameters listingParameters = new ListingParameters(0); // no limit
        listingParameters.addFilterAttributes("createdAt", "01.01.2000");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(DATE_COLLECTION, Job.class, listingParameters);

        for (Job job : listingResult.getResults()) {
            assertEquals(job.toString(), 2000, job.getCreatedAt().getYear());
            assertEquals(job.toString(), 1, job.getCreatedAt().getMonthValue());
            assertEquals(job.toString(), 1, job.getCreatedAt().getDayOfMonth());
        }
        assertEquals(DATE_COLLECTION.size() / DATE_COLLECTION_EXPONENTIATION / 4, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLocalDateTime_DATE_COLLECTION_DDMMYYYY_lastDayInMonth() throws Exception {

        ListingParameters listingParameters = new ListingParameters(0); // no limit
        listingParameters.addFilterAttributes("createdAt", "31.01.2000");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(DATE_COLLECTION, Job.class, listingParameters);

        for (Job job : listingResult.getResults()) {
            assertEquals(job.toString(), 2000, job.getCreatedAt().getYear());
            assertEquals(job.toString(), 1, job.getCreatedAt().getMonthValue());
            assertEquals(job.toString(), 31, job.getCreatedAt().getDayOfMonth());
        }
        assertEquals(1, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLocalDateTime_DATE_COLLECTION_ltYYYY() throws Exception {

        ListingParameters listingParameters = new ListingParameters(0); // no limit
        listingParameters.addFilterAttributes("createdAt", CollectionListing.OPERATOR_LT + "2001");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(DATE_COLLECTION, Job.class, listingParameters);

        for (Job job : listingResult.getResults()) {
            assertTrue(job.toString(), job.getCreatedAt().getYear() < 2001);
        }
        assertEquals(217, listingResult.getResults().size());
    }

    @Ignore // FIXME!
    @Test
    public void testGetListingResult_filterLocalDateTime_DATE_COLLECTION_gtYYYY() throws Exception {

        ListingParameters listingParameters = new ListingParameters(0); // no limit
        listingParameters.addFilterAttributes("createdAt", CollectionListing.OPERATOR_GT + "2001");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(DATE_COLLECTION, Job.class, listingParameters);

        for (Job job : listingResult.getResults()) {
            assertTrue(job.toString(), job.getCreatedAt().getYear() > 2001);
        }
        assertEquals(430, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLong() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(3L);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterLongInvalid() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(3L);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", "c2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(0, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterLongHasNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(null);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterLongLessThan() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(3L);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", CollectionListing.OPERATOR_LT + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
    }

    @Test
    public void testGetListingResult_filterLongGreaterThan() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(3L);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", CollectionListing.OPERATOR_GT + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterLongRange() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(3L);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", "1" + CollectionListing.OPERATOR_TO + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterLongLike() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setId(1234L);
        collection.add(job1);
        Job job2 = new Job();
        job2.setId(2345L);
        collection.add(job2);
        Job job3 = new Job();
        job3.setId(3456L);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("id", CollectionListing.OPERATOR_LIKE + "234");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterInteger() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setMaxPerMinute(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setMaxPerMinute(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setMaxPerMinute(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("maxPerMinute", "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterIntegerHasNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setMaxPerMinute(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setMaxPerMinute(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setMaxPerMinute(null);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("maxPerMinute", "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterIntegerLessThan() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setMaxPerMinute(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setMaxPerMinute(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setMaxPerMinute(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("maxPerMinute", CollectionListing.OPERATOR_LT + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
    }

    @Test
    public void testGetListingResult_filterIntegerGreaterThan() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setMaxPerMinute(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setMaxPerMinute(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setMaxPerMinute(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("maxPerMinute", CollectionListing.OPERATOR_GT + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterIntegerRange() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setMaxPerMinute(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setMaxPerMinute(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setMaxPerMinute(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("maxPerMinute", "1" + CollectionListing.OPERATOR_TO + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterIntegerLike() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setMaxPerMinute(1234);
        collection.add(job1);
        Job job2 = new Job();
        job2.setMaxPerMinute(2345);
        collection.add(job2);
        Job job3 = new Job();
        job3.setMaxPerMinute(3456);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("maxPerMinute", CollectionListing.OPERATOR_LIKE + "234");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterInt() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setThreads(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setThreads(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setThreads(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("threads", "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterIntLessThan() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setThreads(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setThreads(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setThreads(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("threads", CollectionListing.OPERATOR_LT + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
    }

    @Test
    public void testGetListingResult_filterIntGreaterThan() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setThreads(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setThreads(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setThreads(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("threads", CollectionListing.OPERATOR_GT + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_filterIntRange() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setThreads(1);
        collection.add(job1);
        Job job2 = new Job();
        job2.setThreads(2);
        collection.add(job2);
        Job job3 = new Job();
        job3.setThreads(3);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("threads", "1" + CollectionListing.OPERATOR_TO + "2");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterIntLike() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setThreads(1234);
        collection.add(job1);
        Job job2 = new Job();
        job2.setThreads(2345);
        collection.add(job2);
        Job job3 = new Job();
        job3.setThreads(3456);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("threads", CollectionListing.OPERATOR_LIKE + "234");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterBooleanTrue() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setUniqueQueued(false);
        collection.add(job1);
        Job job2 = new Job();
        job2.setUniqueQueued(true);
        collection.add(job2);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("uniqueQueued", CollectionListing.BOOLEAN_TRUE);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
    }

    @Test
    public void testGetListingResult_filterBooleanFalse() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setUniqueQueued(false);
        collection.add(job1);
        Job job2 = new Job();
        job2.setUniqueQueued(true);
        collection.add(job2);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("uniqueQueued", CollectionListing.BOOLEAN_FALSE);

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
    }

    @Test
    public void testGetListingResult_filterBooleanBullshit() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setUniqueQueued(false);
        collection.add(job1);
        Job job2 = new Job();
        job2.setUniqueQueued(true);
        collection.add(job2);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("uniqueQueued", "Bullshit");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(0, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterEnum() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setStatus(JobStatus.ACTIVE);
        collection.add(job1);
        Job job2 = new Job();
        job2.setStatus(JobStatus.ERROR);
        collection.add(job2);
        Job job3 = new Job();
        job3.setStatus(JobStatus.INACTIVE);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("status", "\"ACTIVE\"");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(1, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
    }

    @Test
    public void testGetListingResult_filterEnumOR() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setStatus(JobStatus.ACTIVE);
        collection.add(job1);
        Job job2 = new Job();
        job2.setStatus(JobStatus.ERROR);
        collection.add(job2);
        Job job3 = new Job();
        job3.setStatus(JobStatus.INACTIVE);
        collection.add(job3);
        Job job4 = new Job();
        job4.setStatus(JobStatus.NO_WORKER);
        collection.add(job4);
        Job job5 = new Job();
        job5.setStatus(JobStatus.ACTIVE);
        collection.add(job5);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("status", "\"ACTIVE\"|\"ERROR\"");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(3, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job5));
    }

    @Test
    public void testGetListingResult_filterEnumOR_Pipe() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setStatus(JobStatus.ACTIVE);
        collection.add(job1);
        Job job2 = new Job();
        job2.setStatus(JobStatus.ERROR);
        collection.add(job2);
        Job job3 = new Job();
        job3.setStatus(JobStatus.INACTIVE);
        collection.add(job3);
        Job job4 = new Job();
        job4.setStatus(JobStatus.NO_WORKER);
        collection.add(job4);
        Job job5 = new Job();
        job5.setStatus(JobStatus.ACTIVE);
        collection.add(job5);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("status", "\"ACTIVE\"|\"ERROR\"|");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(3, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job5));
    }

    @Test
    public void testGetListingResult_filterEnumORUnquoted() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setStatus(JobStatus.ACTIVE);
        collection.add(job1);
        Job job2 = new Job();
        job2.setStatus(JobStatus.ERROR);
        collection.add(job2);
        Job job3 = new Job();
        job3.setStatus(JobStatus.INACTIVE);
        collection.add(job3);
        Job job4 = new Job();
        job4.setStatus(JobStatus.NO_WORKER);
        collection.add(job4);
        Job job5 = new Job();
        job5.setStatus(JobStatus.ACTIVE);
        collection.add(job5);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("status", "ACTIVE|ERROR");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(4, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job3));
        assertTrue(listingResult.getResults().contains(job5));
    }

    @Test
    public void testGetListingResult_filterEnumInvalid() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setStatus(JobStatus.ACTIVE);
        collection.add(job1);
        Job job2 = new Job();
        job2.setStatus(JobStatus.ERROR);
        collection.add(job2);
        Job job3 = new Job();
        job3.setStatus(JobStatus.INACTIVE);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("status", "\"active\""); // Enum value is case sensitive!

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(0, listingResult.getResults().size());
    }

    @Test
    public void testGetListingResult_filterEnumLike() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setStatus(JobStatus.ACTIVE);
        collection.add(job1);
        Job job2 = new Job();
        job2.setStatus(JobStatus.ERROR);
        collection.add(job2);
        Job job3 = new Job();
        job3.setStatus(JobStatus.INACTIVE);
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("status", "ACTIVE");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_orInFilter() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "yyy" + CollectionListing.OPERATOR_OR + "zzz");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_orInFilterLike() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name", "y" + CollectionListing.OPERATOR_OR + "z");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job2));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_orInAttribute() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        job1.setDescription("yyy");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        job2.setDescription("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        job3.setDescription("xxx");
        collection.add(job3);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name" + CollectionListing.OPERATOR_OR + "description", "xxx");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(2, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job3));
    }

    @Test
    public void testGetListingResult_orInAttributeAndFilter() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        job1.setDescription("yyy");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        job2.setDescription("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        job3.setDescription("xxx");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("zzz");
        job4.setDescription("zzz");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name" + CollectionListing.OPERATOR_OR + "description", "xxx" + CollectionListing.OPERATOR_OR + "zzz");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(3, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job3));
        assertTrue(listingResult.getResults().contains(job4));
    }

    @Test
    public void testGetListingResult_orInAttributeAndFilter_WordOperator() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("xxx");
        job1.setDescription("yyy");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("yyy");
        job2.setDescription("yyy");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("zzz");
        job3.setDescription("xxx");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("zzz");
        job4.setDescription("zzz");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.addFilterAttributes("name" + CollectionListing.OPERATOR_OR_WORD + "description", "xxx" + CollectionListing.OPERATOR_OR_WORD + "zzz");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(3, listingResult.getResults().size());
        assertTrue(listingResult.getResults().contains(job1));
        assertTrue(listingResult.getResults().contains(job3));
        assertTrue(listingResult.getResults().contains(job4));
    }

    @Test
    public void testGetListingResult_sort() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("3");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("4");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("2");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("1");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.setSortAttribute("name");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(4, listingResult.getResults().size());
        assertEquals(job4, listingResult.getResults().get(0));
        assertEquals(job3, listingResult.getResults().get(1));
        assertEquals(job1, listingResult.getResults().get(2));
        assertEquals(job2, listingResult.getResults().get(3));
    }

    @Test
    public void testGetListingResult_sortPrimitiveType() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setThreads(3);
        collection.add(job1);
        Job job2 = new Job();
        job2.setThreads(4);
        collection.add(job2);
        Job job3 = new Job();
        job3.setThreads(2);
        collection.add(job3);
        Job job4 = new Job();
        job4.setThreads(1);
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.setSortAttribute("threads");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(4, listingResult.getResults().size());
        assertEquals(job4, listingResult.getResults().get(0));
        assertEquals(job3, listingResult.getResults().get(1));
        assertEquals(job1, listingResult.getResults().get(2));
        assertEquals(job2, listingResult.getResults().get(3));
    }

    @Test
    public void testGetListingResult_sortDesc() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("3");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("4");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName("2");
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("1");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.setSortAttribute(CollectionListing.SORT_DESC + "name");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(4, listingResult.getResults().size());
        assertEquals(job2, listingResult.getResults().get(0));
        assertEquals(job1, listingResult.getResults().get(1));
        assertEquals(job3, listingResult.getResults().get(2));
        assertEquals(job4, listingResult.getResults().get(3));
    }

    @Test
    public void testGetListingResult_sort_hasNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("3");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("4");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName(null);
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("1");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.setSortAttribute("name");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(4, listingResult.getResults().size());
        assertEquals(job3, listingResult.getResults().get(0)); // nulls first
        assertEquals(job4, listingResult.getResults().get(1));
        assertEquals(job1, listingResult.getResults().get(2));
        assertEquals(job2, listingResult.getResults().get(3));
    }

    @Test
    public void testGetListingResult_sortDesc_hasNull() throws Exception {

        Set<Job> collection = new HashSet<>();
        Job job1 = new Job();
        job1.setName("3");
        collection.add(job1);
        Job job2 = new Job();
        job2.setName("4");
        collection.add(job2);
        Job job3 = new Job();
        job3.setName(null);
        collection.add(job3);
        Job job4 = new Job();
        job4.setName("1");
        collection.add(job4);

        ListingParameters listingParameters = new ListingParameters();
        listingParameters.setSortAttribute(CollectionListing.SORT_DESC + "name");

        ListingResult<Job> listingResult = CollectionListing.getListingResult(collection, Job.class, listingParameters);

        assertEquals(4, listingResult.getResults().size());
        assertEquals(job2, listingResult.getResults().get(0));
        assertEquals(job1, listingResult.getResults().get(1));
        assertEquals(job4, listingResult.getResults().get(2));
        assertEquals(job3, listingResult.getResults().get(3)); // nulls first desc
    }

}
