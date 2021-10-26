package io.coodoo.workhorse.core.boundary;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
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
        public String doWork(String parameters) throws Exception {
            assertEquals(STRING, parameters);
            return null;
        }
    }
    public class TypeLong extends WorkerWith<Long> {
        @Override
        public String doWork(Long parameters) throws Exception {
            assertEquals(LONG, parameters);
            return null;
        }
    }
    public class TypePojo extends WorkerWith<Pojo> {
        @Override
        public String doWork(Pojo parameters) throws Exception {
            assertEquals(POJO, parameters);
            return null;
        }
    }
    public class TypeListLong extends WorkerWith<List<Long>> {
        @Override
        public String doWork(List<Long> parameters) throws Exception {
            assertEquals(LONG_LIST.toString(), parameters.toString());
            return null;
        }
    }
    public class TypeListString extends WorkerWith<List<String>> {
        @Override
        public String doWork(List<String> parameters) throws Exception {
            assertEquals(STRING_LIST.toString(), parameters.toString());
            return null;
        }
    }
    public class TypeListPojo extends WorkerWith<List<Pojo>> {
        @Override
        public String doWork(List<Pojo> parameters) throws Exception {
            assertEquals(POJO_LIST.toString(), parameters.toString());
            return null;
        }
    }
    public class TypeSetString extends WorkerWith<Set<String>> {
        @Override
        public String doWork(Set<String> parameters) throws Exception {
            assertEquals(STRING_SET.toString(), parameters.toString());
            return null;
        }
    }
    public class TypeMapLongString extends WorkerWith<Map<Long, String>> {
        @Override
        public String doWork(Map<Long, String> parameters) throws Exception {
            assertEquals(LONG_STRING_MAP.toString(), parameters.toString());
            return null;
        }
    }

    @Mock
    private ExecutionContext jobContext;

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

    @Test
    public void testGetParametersListString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(STRING_LIST);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        List<String> result = workerWithListString.getParameters(jobExecution);

        assertEquals(STRING_LIST, result);
    }

    @Test
    public void testGetParametersClassListString() throws Exception {

        Class<?> result = workerWithListString.getParametersClass();

        assertEquals(List.class, result);
    }

    @Test
    public void testDoWorkListString() throws Exception {

        workerWithListString.doWork(STRING_LIST);
    }

    @Test
    public void testGetParametersListLong() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(LONG_LIST);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        List<Long> result = workerWithListLong.getParameters(jobExecution);

        assertEquals(LONG_LIST, result);
    }

    @Test
    public void testGetParametersClassListLong() throws Exception {

        Class<?> result = workerWithListLong.getParametersClass();

        assertEquals(List.class, result);
    }

    @Test
    public void testDoWorkListLong() throws Exception {

        workerWithListLong.doWork(LONG_LIST);
    }

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

    @Test
    public void testGetParametersSetString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(STRING_SET);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        Set<String> result = workerWithSetString.getParameters(jobExecution);

        assertEquals(STRING_SET, result);
    }

    @Test
    public void testGetParametersClassSetString() throws Exception {

        Class<?> result = workerWithSetString.getParametersClass();

        assertEquals(Set.class, result);
    }

    @Test
    public void testDoWorkSetString() throws Exception {

        workerWithSetString.doWork(STRING_SET);
    }

    @Test
    public void testGetParametersMapLongString() throws Exception {

        String parameters = WorkhorseUtil.parametersToJson(LONG_STRING_MAP);

        Execution jobExecution = new Execution();
        jobExecution.setParameters(parameters);

        Map<Long, String> result = workerWithMapLongString.getParameters(jobExecution);

        assertEquals(LONG_STRING_MAP, result);
    }

    @Test
    public void testGetParametersClassMapLongString() throws Exception {

        Class<?> result = workerWithMapLongString.getParametersClass();

        assertEquals(Map.class, result);
    }

    @Test
    public void testDoWorkMapLongString() throws Exception {

        workerWithMapLongString.doWork(LONG_STRING_MAP);
    }

}
