package io.coodoo.workhorse.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.coodoo.workhorse.core.control.StaticConfig;

public class WorkhorseUtilTest {

    @Test
    public void testGetMessagesFromException() throws Exception {

        Exception exception = new Exception();

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals(exception.getClass().getName(), result);
    }

    @Test
    public void testGetMessagesFromException_message() throws Exception {

        String message = "DAFUQ!!!";
        Exception exception = new Exception("DAFUQ!!!");

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals(message, result);
    }

    @Test
    public void testGetMessagesFromException_causedByNullPointerException() throws Exception {

        NullPointerException nullPointerException = new NullPointerException();
        Exception exception = new Exception(nullPointerException);

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals(nullPointerException.getClass().getName(), result);
    }

    @Test
    public void testGetMessagesFromException_causedByNullPointerException_b() throws Exception {

        Exception exception = new Exception(new NullPointerException("b"));

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("b", result);
    }

    @Test
    public void testGetMessagesFromException_causedByNullPointerException_a_b() throws Exception {

        Exception exception = new Exception("a", new NullPointerException("b"));

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("a | b", result);
    }

    @Test
    public void testGetMessagesFromException_causedByNullPointerException_a() throws Exception {

        Exception exception = new Exception("a", new NullPointerException());

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("a", result);
    }

    @Test
    public void testGetMessagesFromException_NullPointerException() throws Exception {

        NullPointerException nullPointerException = new NullPointerException();

        String result = WorkhorseUtil.getMessagesFromException(nullPointerException);

        assertEquals(nullPointerException.getClass().getName(), result);
    }

    @Test
    public void testGetMessagesFromException_NullPointerException_message() throws Exception {

        String message = "DAFUQ!!!";
        NullPointerException nullPointerException = new NullPointerException("DAFUQ!!!");

        String result = WorkhorseUtil.getMessagesFromException(nullPointerException);

        assertEquals(message, result);
    }

    @Test
    public void testGetMessagesFromException_a_b_c_d_e() throws Exception {

        Exception exception = new Exception("a", new Exception("b", new Exception("c", new Exception("d", new Exception("e")))));

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("a | b | c | d | e", result);
    }

    @Test
    public void testGetMessagesFromException_a_b_b_c_c() throws Exception {

        Exception exception = new Exception("a", new Exception("b", new Exception("b", new Exception("c", new Exception("c")))));

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("a | b | c", result);
    }

    @Test
    public void testGetMessagesFromException_a_b_c_c_b() throws Exception {

        Exception exception = new Exception("a", new Exception("b", new Exception("c", new Exception("c", new Exception("b")))));

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("a | b | c", result);
    }

    @Test
    public void testGetMessagesFromException_a_b_c_NullPointerException() throws Exception {

        NullPointerException nullPointerException = new NullPointerException();
        Exception exception = new Exception("a", new Exception("b", new Exception("c", nullPointerException)));

        String result = WorkhorseUtil.getMessagesFromException(exception);

        assertEquals("a | b | c", result);
    }

    @Test
    public void testDelayToMaturity() throws Exception {
        StaticConfig.TIME_ZONE = "UTC";
        LocalDateTime result = WorkhorseUtil.delayToMaturity(4L, ChronoUnit.SECONDS);

        assertNotNull(result);
    }

    @Test
    public void testDelayToMaturity_parameters_null() throws Exception {

        LocalDateTime result = WorkhorseUtil.delayToMaturity(null, null);

        assertNull(result);
    }

    @Test
    public void testDelayToMaturity_delayValue_null() throws Exception {

        LocalDateTime result = WorkhorseUtil.delayToMaturity(null, ChronoUnit.SECONDS);

        assertNull(result);
    }

    @Test
    public void testDelayToMaturity_chronoUnit_null() throws Exception {

        LocalDateTime result = WorkhorseUtil.delayToMaturity(4L, null);

        assertNull(result);
    }

}
