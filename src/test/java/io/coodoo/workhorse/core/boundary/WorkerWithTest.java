package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.coodoo.workhorse.core.entity.Execution;
import io.coodoo.workhorse.util.WorkhorseUtil;

@RunWith(MockitoJUnitRunner.class)
public class WorkerWithTest {

    private static String STRING;
    private static Long LONG;
    private static Pojo POJO;
    private static List<String> STRING_LIST;
    private static List<Long> LONG_LIST;
    private static List<Pojo> POJO_LIST;
    private static Set<String> STRING_SET;
    private static Map<Long, String> LONG_STRING_MAP;

    public class TypeString extends WorkerWith<String> {
        @Override
        public void doWork(String parameters) throws Exception {
            assertEquals(STRING, parameters);
        }
    }
    public class TypeLong extends WorkerWith<Long> {
        @Override
        public void doWork(Long parameters) throws Exception {
            assertEquals(LONG, parameters);
        }
    }
    public class TypePojo extends WorkerWith<Pojo> {
        @Override
        public void doWork(Pojo parameters) throws Exception {
            assertEquals(POJO, parameters);
        }
    }
    public class TypeListLong extends WorkerWith<List<Long>> {
        @Override
        public void doWork(List<Long> parameters) throws Exception {
            assertEquals(LONG_LIST.toString(), parameters.toString());
        }
    }
    public class TypeListString extends WorkerWith<List<String>> {
        @Override
        public void doWork(List<String> parameters) throws Exception {
            assertEquals(STRING_LIST.toString(), parameters.toString());
        }
    }
    public class TypeListPojo extends WorkerWith<List<Pojo>> {
        @Override
        public void doWork(List<Pojo> parameters) throws Exception {
            assertEquals(POJO_LIST.toString(), parameters.toString());
        }
    }
    public class TypeSetString extends WorkerWith<Set<String>> {
        @Override
        public void doWork(Set<String> parameters) throws Exception {
            assertEquals(STRING_SET.toString(), parameters.toString());
        }
    }
    public class TypeMapLongString extends WorkerWith<Map<Long, String>> {
        @Override
        public void doWork(Map<Long, String> parameters) throws Exception {
            assertEquals(LONG_STRING_MAP.toString(), parameters.toString());
        }
    }

    @Mock
    private JobContext jobContext;

    @Mock
    private WorkhorseService jobEngineService;

    @InjectMocks
    private TypeString workerWithString = new TypeString();

    @InjectMocks
    private TypeLong workerWithLong = new TypeLong();

    @InjectMocks
    private TypePojo workerWithPojo = new TypePojo();

    @InjectMocks
    private TypeListString workerWithListString = new TypeListString();

    @InjectMocks
    private TypeListLong workerWithListLong = new TypeListLong();

    @InjectMocks
    private TypeListPojo workerWithListPojo = new TypeListPojo();

    @InjectMocks
    private TypeSetString workerWithSetString = new TypeSetString();

    @InjectMocks
    private TypeMapLongString workerWithMapLongString = new TypeMapLongString();

    @Before
    public void setUp() {
        STRING = ";)";
        LONG = new Long(83L);
        POJO = new Pojo();
        STRING_LIST = new ArrayList<>();
        LONG_LIST = new ArrayList<>();
        POJO_LIST = new ArrayList<>();
        STRING_SET = new HashSet<>();
        LONG_STRING_MAP = new HashMap<>();
    }

    @Test
    public void testGetParametersString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(STRING);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        String result = workerWithString.getParameters(jobExecution);

        assertEquals(STRING, result);
    }

    @Test
    public void testGetParametersClassString() throws Exception {

        Class<?> result = workerWithString.getParametersClass();

        assertEquals(String.class, result);
    }

    @Test
    public void testDoWorkString() throws Exception {

        workerWithString.doWork(STRING);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionString() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING));
        workerWithString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Test
    public void testGetParametersLong() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(LONG);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        Long result = workerWithLong.getParameters(jobExecution);

        assertEquals(LONG, result);
    }

    @Test
    public void testGetParametersClassLong() throws Exception {

        Class<?> result = workerWithLong.getParametersClass();

        assertEquals(Long.class, result);
    }

    @Test
    public void testDoWorkLong() throws Exception {

        workerWithLong.doWork(LONG);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionLong() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG));
        workerWithLong.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Test
    public void testGetParametersPojo() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(POJO);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        Pojo result = workerWithPojo.getParameters(jobExecution);

        assertEquals(POJO, result);
    }

    @Test
    public void testGetParametersClassPojo() throws Exception {

        Class<?> result = workerWithPojo.getParametersClass();

        assertEquals(Pojo.class, result);
    }

    @Test
    public void testDoWorkPojo() throws Exception {

        POJO = new Pojo();

        workerWithPojo.doWork(POJO);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionPojoEmpty() throws Exception {

        POJO = new Pojo();

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(POJO));
        workerWithPojo.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Test
    public void testDoWorkExecutionPojoFull() throws Exception {

        POJO = new Pojo();
        POJO.i = -2;
        POJO.io = new Integer(32456);
        POJO.setL(987654L);
        POJO.setLo(new Long(-1));
        POJO.s = "Stringily";
        POJO.b = true;
        POJO.bo = Boolean.FALSE;
        POJO.d = new Date();
        POJO.lt = LocalTime.now();
        POJO.ldt = LocalDateTime.now();
        Pojo mojoPojo = new Pojo();
        mojoPojo.setL(LONG);
        POJO.mp = mojoPojo;
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("");
        list.add(null);
        POJO.ls = list;
        Map<Long, String> map = new HashMap<>();
        map.put(0L, "value");
        map.put(1L, null);
        POJO.mls = map;
        POJO.ia = new int[] {10, 20, 30, 40, 50, 60, 71, 80, 90, 91};

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(POJO));
        workerWithPojo.doWork(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListString() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING_LIST));
        workerWithListString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testGetParametersListString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(STRING_LIST);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        List<String> result = workerWithListString.getParameters(jobExecution);

        assertEquals(STRING_LIST, result);
    }

    @Ignore
    @Test
    public void testGetParametersClassListString() throws Exception {

        Class<?> result = workerWithListString.getParametersClass();

        assertEquals(List.class, result);
    }

    @Test
    public void testDoWorkListString() throws Exception {

        workerWithListString.doWork(STRING_LIST);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListStringEmpty() throws Exception {

        STRING_LIST = new ArrayList<>();

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING_LIST));
        workerWithListString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListStringFull() throws Exception {

        STRING_LIST = new ArrayList<>();
        STRING_LIST.add("x");
        STRING_LIST.add("");
        STRING_LIST.add(null);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING_LIST));
        workerWithListString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListLong() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG_LIST));
        workerWithListLong.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testGetParametersListLong() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(LONG_LIST);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        List<Long> result = workerWithListLong.getParameters(jobExecution);

        assertEquals(LONG_LIST, result);
    }

    @Ignore
    @Test
    public void testGetParametersClassListLong() throws Exception {

        Class<?> result = workerWithListLong.getParametersClass();

        assertEquals(List.class, result);
    }

    @Test
    public void testDoWorkListLong() throws Exception {

        workerWithListLong.doWork(LONG_LIST);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListLongEmpty() throws Exception {

        LONG_LIST = new ArrayList<>();

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG_LIST));
        workerWithListLong.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListLongFull() throws Exception {

        LONG_LIST = new ArrayList<>();
        LONG_LIST.add(-1L);
        LONG_LIST.add(0L);
        LONG_LIST.add(null);
        LONG_LIST.add(1L);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG_LIST));
        workerWithListLong.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListPojo() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(POJO_LIST));
        workerWithListPojo.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testGetParametersListPojo() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(POJO_LIST);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        List<Pojo> result = workerWithListPojo.getParameters(jobExecution);

        assertEquals(POJO_LIST, result);
    }

    @Test
    public void testDoWorkListPojo() throws Exception {

        workerWithListPojo.doWork(POJO_LIST);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionListPojoEmpty() throws Exception {

        POJO_LIST = new ArrayList<>();

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(POJO_LIST));
        workerWithListPojo.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore // Lists only work with primitive Java objects...
    @Test
    public void testDoWorkExecutionListPojoFull() throws Exception {

        POJO = new Pojo();
        POJO.i = -2;
        POJO.io = new Integer(32456);
        POJO.setL(987654L);
        POJO.setLo(new Long(-1));
        POJO.s = "Stringily";
        POJO.b = true;
        POJO.bo = Boolean.FALSE;
        POJO.d = new Date();
        POJO.lt = LocalTime.now();
        POJO.ldt = LocalDateTime.now();
        Pojo mojoPojo = new Pojo();
        mojoPojo.setL(LONG);
        POJO.mp = mojoPojo;
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("");
        list.add(null);
        POJO.ls = list;
        Map<Long, String> map = new HashMap<>();
        map.put(0L, "value");
        map.put(1L, null);
        POJO.mls = map;
        POJO.ia = new int[] {10, 20, 30, 40, 50, 60, 71, 80, 90, 91};

        POJO_LIST = new ArrayList<>();
        POJO_LIST.add(POJO);
        POJO_LIST.add(null);
        POJO_LIST.add(new Pojo());

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(POJO_LIST));
        workerWithListPojo.doWork(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionSetString() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING_SET));
        workerWithSetString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testGetParametersSetString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(STRING_SET);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        Set<String> result = workerWithSetString.getParameters(jobExecution);

        assertEquals(STRING_SET, result);
    }

    @Ignore
    @Test
    public void testGetParametersClassSetString() throws Exception {

        Class<?> result = workerWithSetString.getParametersClass();

        assertEquals(Set.class, result);
    }

    @Test
    public void testDoWorkSetString() throws Exception {

        workerWithSetString.doWork(STRING_SET);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionSetStringEmpty() throws Exception {

        STRING_SET = new HashSet<>();

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING_SET));
        workerWithSetString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionSetStringFull() throws Exception {

        STRING_SET = new HashSet<>();
        STRING_SET.add("x");
        STRING_SET.add("");
        STRING_SET.add(null);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(STRING_SET));
        workerWithSetString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionMapLongString() throws Exception {

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG_STRING_MAP));
        workerWithMapLongString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testGetParametersMapLongString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(LONG_STRING_MAP);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        Map<Long, String> result = workerWithMapLongString.getParameters(jobExecution);

        assertEquals(LONG_STRING_MAP, result);
    }

    @Ignore
    @Test
    public void testGetParametersClassMapLongString() throws Exception {

        Class<?> result = workerWithMapLongString.getParametersClass();

        assertEquals(Map.class, result);
    }

    @Test
    public void testDoWorkMapLongString() throws Exception {

        workerWithMapLongString.doWork(LONG_STRING_MAP);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionMapLongStringEmpty() throws Exception {

        LONG_STRING_MAP = new HashMap<>();

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG_STRING_MAP));
        workerWithMapLongString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }

    @Ignore
    @Test
    public void testDoWorkExecutionMapLongStringFull() throws Exception {

        LONG_STRING_MAP = new HashMap<>();
        LONG_STRING_MAP.put(-1L, "x");
        LONG_STRING_MAP.put(0L, "");
        LONG_STRING_MAP.put(1L, null);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(WorkhorseUtil.parametersToJson(LONG_STRING_MAP));
        workerWithMapLongString.doWork(jobExecution);

        verify(jobContext).init(jobExecution);
    }
}
