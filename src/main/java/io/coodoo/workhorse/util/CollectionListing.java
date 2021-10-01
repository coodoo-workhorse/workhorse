package io.coodoo.workhorse.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.coodoo.workhorse.persistence.interfaces.listing.ListingParameters;
import io.coodoo.workhorse.persistence.interfaces.listing.ListingResult;
import io.coodoo.workhorse.persistence.interfaces.listing.Metadata;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class CollectionListing {

    private static Logger log = LoggerFactory.getLogger(CollectionListing.class);

    private static final String REGEX_LONG = "[-+]?\\d{1,37}";
    private static final String REGEX_INT = "[-+]?\\d{1,10}";
    private static final String REGEX_SHORT = "[-+]?\\d{1,5}";
    private static final String REGEX_FLOAT = "[-+]?\\d*[.,]?\\d+";
    private static final String REGEX_DOUBLE = "[-+]?\\d*[.,]?\\d+";
    // _______________________Group_numbers:__12______________34______________5_________
    private static final String REGEX_DATE = "((\\d{1,2})\\D)?((\\d{1,2})\\D)?(\\d{2,})";

    private static final String EMPTY = "";
    private static final String BLANC = " ";

    /**
     * Default index for pagination
     */
    public static int DEFAULT_INDEX = 0;

    /**
     * Default current page number for pagination
     */
    public static int DEFAULT_PAGE = 1;

    /**
     * Default limit of results per page for pagination
     */
    public static int DEFAULT_LIMIT = 10;

    /**
     * If this key is present in filterAttributes map, the attributes gets disjuncted (default is conjunction)
     */
    public static String FILTER_TYPE_DISJUNCTION = "Filter-Type-Disjunction";

    /**
     * Limit on OR operator separated predicated to handle it in an IN statement
     */
    public static int OR_LIMIT = 10;

    /**
     * NOT operator
     */
    public static String OPERATOR_NOT = "!";

    /**
     * NOT operator as word
     */
    private static String OPERATOR_NOT_WORD_BLANK = "NOT";
    public static String OPERATOR_NOT_WORD = OPERATOR_NOT_WORD_BLANK + BLANC;

    /**
     * OR operator
     */
    public static String OPERATOR_OR = "|";

    /**
     * OR operator as word
     */
    private static String OPERATOR_OR_WORD_BLANK = "OR";
    public static String OPERATOR_OR_WORD = BLANC + OPERATOR_OR_WORD_BLANK + BLANC;

    // TODO
    // /**
    // * AND operator
    // */
    // public static String OPERATOR_AND = "&";
    //
    // /**
    // * AND operator as word
    // */
    // private static String OPERATOR_AND_WORD_BLANK = "AND";
    // public static String OPERATOR_AND_WORD = BLANC + OPERATOR_AND_WORD_BLANK + BLANC;

    /**
     * LIKE operator
     */
    public static String OPERATOR_LIKE = "~";

    /**
     * LIKE operator as word
     */
    private static String OPERATOR_LIKE_WORD_BLANK = "LIKE";
    public static String OPERATOR_LIKE_WORD = OPERATOR_LIKE_WORD_BLANK + BLANC;

    /**
     * LESS THAN operator
     */
    public static String OPERATOR_LT = "<";

    /**
     * LESS THAN operator as word
     */
    private static String OPERATOR_LT_WORD_BLANK = "LT";
    public static String OPERATOR_LT_WORD = OPERATOR_LT_WORD_BLANK + BLANC;

    /**
     * GREATER THAN operator
     */
    public static String OPERATOR_GT = ">";

    /**
     * GREATER THAN operator as word
     */
    private static String OPERATOR_GT_WORD_BLANK = "GT";
    public static String OPERATOR_GT_WORD = OPERATOR_GT_WORD_BLANK + BLANC;

    /**
     * TO operator
     */
    public static String OPERATOR_TO = "-";

    /**
     * TO operator as word
     */
    private static String OPERATOR_TO_WORD_BLANK = "TO";
    public static String OPERATOR_TO_WORD = BLANC + OPERATOR_TO_WORD_BLANK + BLANC;

    /**
     * NULL operator
     */
    public static String OPERATOR_NULL = "NULL";

    /**
     * Wildcard many
     */
    public static String WILDCARD_MANY = "*";

    /**
     * Wildcard one
     */
    public static String WILDCARD_ONE = "?";

    /**
     * ASC Sort direction operator
     */
    public static String SORT_ASC = "+";

    /**
     * DESC Sort direction operator
     */
    public static String SORT_DESC = "-";

    /**
     * Filter value to match boolean true
     */
    public static String BOOLEAN_TRUE = "true";

    /**
     * Filter value to match boolean false
     */
    public static String BOOLEAN_FALSE = "false";

    /**
     * Gets the listing result
     * 
     * <h3>URL query parameters</h3>
     * <ul>
     * <li><strong>filter</strong>: The filter value gets applied to every attribute of the table. Every row where a attribute matches this filter will be part
     * of the result (disjunctive). It can be used as a sort of global search on a Table.</li>
     * 
     * <li><strong>filter-<i>xxx</i></strong>: filter attributes where <i>xxx</i> is the row name (attribute of the target entity) and the filter value the
     * filter for that row. Every row where all filter attributes matches will be part of the result (conjunctive).</li>
     * 
     * <li><strong>sort</strong>: Given a row name will sort the result in ascending order, to get a descending sorted result a the row name must start with
     * "-"</li>
     * </ul>
     * <h3>Pagination</h3>
     * <ul>
     * <li><strong>limit</strong>: Amount of maximal expected results per request to fit on a page (default = 10)</li>
     * <li><strong>page</strong>: Current page (default = 1)</li>
     * <li><strong>index</strong>: Index (default = 0)</li>
     * </ul>
     * 
     * @param <T> type of target entity class
     * @param entityManager entity manager of designated persistence unit
     * @param clazz target entity class
     * @param parameters defines the listing queue. It contains optional query parameters as described above
     * @return a {@link ListingResult} object containing metadata and the resulting list of the target entity instances (sublist in case of pagination)
     *         <h3>{@link Metadata}</h3>
     *         <ul>
     *         <li><strong>count</strong>: Amount of matching results</li>
     *         <li><strong>currentPage</strong>: Current page (pagination)</li>
     *         <li><strong>numPages</strong>: Amount of available pages (pagination)</li>
     *         <li><strong>limit</strong>: Amount of given result for the current page, see list results (pagination, if 0 then there is no limit)</li>
     *         <li><strong>sort</strong>: Current order by this row (ascending except if it starts with "-" = descending)</li>
     *         <li><strong>startIndex</strong>: Index of the first result in this page (pagination)</li>
     *         <li><strong>endIndex</strong>: Index of the last result in this page (pagination)</li>
     *         </ul>
     */
    public static <T> ListingResult<T> getListingResult(Collection<T> collection, Class<T> clazz, ListingParameters listingParameters) {

        List<Predicate<T>> allPredicates = new ArrayList<Predicate<T>>();
        Map<String, Field> fields = getFields(clazz);
        Map<String, String> filterAttributes = listingParameters.getFilterAttributes();

        if (filterAttributes != null && !filterAttributes.isEmpty()) {

            for (String attribute : filterAttributes.keySet()) {
                if (attribute == null || attribute.isEmpty()) {
                    continue;
                }
                String filter = filterAttributes.get(attribute);

                if (filter == null || filter.isEmpty()) {
                    continue;
                }
                if (attribute.contains(OPERATOR_OR) || attribute.contains(OPERATOR_OR_WORD)) {
                    // a filter can be applied on many fields, joined by a "|" (OPERATOR_OR), those get conjuncted
                    List<String> orAttributes = splitOr(attribute.replaceAll(escape(OPERATOR_OR_WORD), OPERATOR_OR));

                    // many attributes for one or many filter
                    List<Predicate<T>> orPredicates = new ArrayList<Predicate<T>>();
                    for (String orAttribute : orAttributes) {
                        Field field = fields.get(orAttribute);
                        if (field == null) {
                            continue;
                        }
                        if (filter.contains(OPERATOR_OR) || filter.contains(OPERATOR_OR_WORD)) {
                            String escapedFilter = filter.replaceAll(escape(OPERATOR_OR_WORD), OPERATOR_OR).replaceAll(escape(OPERATOR_OR), "|");
                            orPredicates.add(createPredicate(clazz, field, escapedFilter));
                        } else {
                            orPredicates.add(createPredicate(clazz, field, filter));
                        }
                    }
                    allPredicates.add(orPredicates.stream().reduce(x -> false, Predicate::or));
                } else {
                    Field field = fields.get(attribute);
                    if (field == null) {
                        continue;
                    }
                    if (filter.contains(OPERATOR_OR) || filter.contains(OPERATOR_OR_WORD)) {
                        // one attribute for many filter
                        List<String> orFilters = splitOr(filter.replaceAll(escape(OPERATOR_OR_WORD), OPERATOR_OR));
                        allPredicates.add(orFilters.stream().map(f -> createPredicate(clazz, field, f)).reduce(x -> false, Predicate::or));
                    } else {
                        // one attribute for one filter
                        allPredicates.add(createPredicate(clazz, field, filter));
                    }
                }
            }
        }

        List<T> filteredList = collection.stream().filter(allPredicates.stream().reduce(x -> true, Predicate::and)).collect(Collectors.toList());

        sort(clazz, listingParameters, fields, filteredList);

        Metadata metadata = new Metadata(new Long(filteredList.size()), listingParameters);

        if (!filteredList.isEmpty() && filteredList.size() > listingParameters.getLimit()) {
            filteredList = filteredList.subList(metadata.getStartIndex() - 1, metadata.getEndIndex());
        }
        return new ListingResult<T>(filteredList, metadata);
    }

    private static <T> void sort(Class<T> clazz, ListingParameters listingParameters, Map<String, Field> fields, List<T> filteredList) {

        String sort = listingParameters.getSortAttribute();

        if (sort != null && !sort.isEmpty()) {
            boolean asc = true;
            if (sort.startsWith(SORT_ASC)) {
                sort = sort.replace(SORT_ASC, EMPTY);
            } else if (sort.startsWith(SORT_DESC)) {
                sort = sort.replace(SORT_DESC, EMPTY);
                asc = false;
            }
            Field field = fields.get(sort);

            if (field != null) {

                if (Comparable.class.isAssignableFrom(field.getType()) || field.getType().isPrimitive()) {
                    Collections.sort(filteredList, new Comparator<T>() {
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        public int compare(T e1, T e2) {
                            try {
                                field.setAccessible(true);
                                Comparable val1 = (Comparable) field.get(e1);
                                Comparable val2 = (Comparable) field.get(e2);

                                if (val1 == null) {
                                    if (val2 == null) {
                                        return 0;
                                    }
                                    return -1;
                                }
                                if (val2 == null) {
                                    return 1;
                                }
                                return val1.compareTo(val2);
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } else {
                    log.error("Can't compare on attribute '{}' on class {}", field.getName(), clazz.getName());
                }
                if (!asc) {
                    Collections.reverse(filteredList);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Predicate<T> createPredicate(Class<T> clazz, Field field, String filter) {

        String fieldName = field.getName();
        String simpleName = field.getType().getSimpleName();

        // Nulls
        if (matches(filter, OPERATOR_NULL)) {
            return instance -> {
                try {
                    field.setAccessible(true);
                    return field.get(instance) == null;
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    return false;
                }
            };
        }

        switch (simpleName) {

            case "String":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        if (object != null) {
                            String string = (String) object;
                            if (isQuoted(filter)) { // quoted values needs an exact match
                                return string.toLowerCase().equals(removeQuotes(filter).toLowerCase());
                            }
                            return string.toLowerCase().matches(likeValue(filter));
                        } else {
                            return false;
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "LocalDateTime":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        LocalDateTime value = (LocalDateTime) object;
                        if (value != null) {
                            long valueMillis = WorkhorseUtil.toEpochMilli(value);
                            if (validDate(filter)) {
                                Long ltMillis = parseDateTimeToMillis(filter, false);
                                Long gtMillis = parseDateTimeToMillis(filter, true);
                                return ltMillis <= valueMillis && valueMillis <= gtMillis;
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validDate(ltFilter)) {
                                    Long filterMillis = parseDateTimeToMillis(ltFilter, false);
                                    return valueMillis < filterMillis.longValue();
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validDate(gtFilter)) {
                                    Long filterMillis = parseDateTimeToMillis(gtFilter, false);
                                    return valueMillis > filterMillis.longValue();
                                }
                            }
                            Matcher dateTimeRange = Pattern.compile(rangePatternDate()).matcher(filter);
                            if (dateTimeRange.find()) {
                                Long ltMillis = parseDateTimeToMillis(dateTimeRange.group(1), false);
                                Long gtMillis = parseDateTimeToMillis(dateTimeRange.group(8), true);
                                return ltMillis <= valueMillis && valueMillis <= gtMillis;
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Date":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Date value = (Date) object;
                        if (value != null) {
                            long valueMillis = value.getTime();
                            if (validDate(filter)) {
                                Long ltMillis = parseDateTimeToMillis(filter, false);
                                Long gtMillis = parseDateTimeToMillis(filter, true);
                                return ltMillis <= valueMillis && valueMillis <= gtMillis;
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validDate(ltFilter)) {
                                    Long filterMillis = parseDateTimeToMillis(ltFilter, false);
                                    return valueMillis < filterMillis.longValue();
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validDate(gtFilter)) {
                                    Long filterMillis = parseDateTimeToMillis(gtFilter, false);
                                    return valueMillis > filterMillis.longValue();
                                }
                            }
                            Matcher dateTimeRange = Pattern.compile(rangePatternDate()).matcher(filter);
                            if (dateTimeRange.find()) {
                                Long ltMillis = parseDateTimeToMillis(dateTimeRange.group(1), false);
                                Long gtMillis = parseDateTimeToMillis(dateTimeRange.group(8), true);
                                return ltMillis <= valueMillis && valueMillis <= gtMillis;
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Long":
            case "long":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Long value = (Long) object;
                        if (value != null) {
                            if (validLong(filter)) {
                                return value.equals(Long.valueOf(filter));
                            }
                            if (filter.startsWith(OPERATOR_LIKE) || filter.startsWith(OPERATOR_LIKE_WORD)) {
                                String likeFilter = filter.replace(OPERATOR_LIKE, EMPTY).replace(OPERATOR_LIKE_WORD, EMPTY);
                                if (validLong(likeFilter)) {
                                    return value.toString().matches(likeValue(likeFilter));
                                }
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validLong(ltFilter)) {
                                    return value.compareTo(Long.valueOf(ltFilter)) < 0;
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validLong(gtFilter)) {
                                    return value.compareTo(Long.valueOf(gtFilter)) > 0;
                                }
                            }
                            Matcher longRange = Pattern.compile(rangePatternLong()).matcher(filter);
                            if (longRange.find()) {
                                return Long.valueOf(longRange.group(1)) <= value && value <= Long.valueOf(longRange.group(3));
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Integer":
            case "int":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Integer value = (Integer) object;
                        if (value != null) {
                            if (validInt(filter)) {
                                return value.equals(Integer.valueOf(filter));
                            }
                            if (filter.startsWith(OPERATOR_LIKE) || filter.startsWith(OPERATOR_LIKE_WORD)) {
                                String likeFilter = filter.replace(OPERATOR_LIKE, EMPTY).replace(OPERATOR_LIKE_WORD, EMPTY);
                                if (validInt(likeFilter)) {
                                    return value.toString().matches(likeValue(likeFilter));
                                }
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validInt(ltFilter)) {
                                    return value.compareTo(Integer.valueOf(ltFilter)) < 0;
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validInt(gtFilter)) {
                                    return value.compareTo(Integer.valueOf(gtFilter)) > 0;
                                }
                            }
                            Matcher intRange = Pattern.compile(rangePatternInt()).matcher(filter);
                            if (intRange.find()) {
                                return Integer.valueOf(intRange.group(1)) <= value && value <= Integer.valueOf(intRange.group(3));
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Short":
            case "short":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Short value = (Short) object;
                        if (value != null) {
                            if (validShort(filter)) {
                                return value.equals(Short.valueOf(filter));
                            }
                            if (filter.startsWith(OPERATOR_LIKE) || filter.startsWith(OPERATOR_LIKE_WORD)) {
                                String likeFilter = filter.replace(OPERATOR_LIKE, EMPTY).replace(OPERATOR_LIKE_WORD, EMPTY);
                                if (validShort(likeFilter)) {
                                    return value.toString().matches(likeValue(likeFilter));
                                }
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validShort(ltFilter)) {
                                    return value.compareTo(Short.valueOf(ltFilter)) < 0;
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validShort(gtFilter)) {
                                    return value.compareTo(Short.valueOf(gtFilter)) > 0;
                                }
                            }
                            Matcher shortRange = Pattern.compile(rangePatternShort()).matcher(filter);
                            if (shortRange.find()) {
                                return Short.valueOf(shortRange.group(1)) <= value && value <= Short.valueOf(shortRange.group(3));
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Float":
            case "float":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Float value = (Float) object;
                        if (value != null) {
                            if (validFloat(filter)) {
                                return value.equals(Float.valueOf(filter));
                            }
                            if (filter.startsWith(OPERATOR_LIKE) || filter.startsWith(OPERATOR_LIKE_WORD)) {
                                String likeFilter = filter.replace(OPERATOR_LIKE, EMPTY).replace(OPERATOR_LIKE_WORD, EMPTY);
                                if (validFloat(likeFilter)) {
                                    return value.toString().matches(likeValue(likeFilter));
                                }
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validFloat(ltFilter)) {
                                    return value.compareTo(Float.valueOf(ltFilter)) < 0;
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validFloat(gtFilter)) {
                                    return value.compareTo(Float.valueOf(gtFilter)) > 0;
                                }
                            }
                            Matcher floatRange = Pattern.compile(rangePatternFloat()).matcher(filter);
                            if (floatRange.find()) {
                                return Float.valueOf(floatRange.group(1)) <= value && value <= Float.valueOf(floatRange.group(3));
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Double":
            case "double":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Double value = (Double) object;
                        if (value != null) {
                            if (validDouble(filter)) {
                                return value.equals(Double.valueOf(filter));
                            }
                            if (filter.startsWith(OPERATOR_LIKE) || filter.startsWith(OPERATOR_LIKE_WORD)) {
                                String likeFilter = filter.replace(OPERATOR_LIKE, EMPTY).replace(OPERATOR_LIKE_WORD, EMPTY);
                                if (validDouble(likeFilter)) {
                                    return value.toString().matches(likeValue(likeFilter));
                                }
                            }
                            if (filter.startsWith(OPERATOR_LT) || filter.startsWith(OPERATOR_LT_WORD)) {
                                String ltFilter = filter.replace(OPERATOR_LT, EMPTY).replace(OPERATOR_LT_WORD, EMPTY);
                                if (validDouble(ltFilter)) {
                                    return value.compareTo(Double.valueOf(ltFilter)) < 0;
                                }
                            }
                            if (filter.startsWith(OPERATOR_GT) || filter.startsWith(OPERATOR_GT_WORD)) {
                                String gtFilter = filter.replace(OPERATOR_GT, EMPTY).replace(OPERATOR_GT_WORD, EMPTY);
                                if (validDouble(gtFilter)) {
                                    return value.compareTo(Double.valueOf(gtFilter)) > 0;
                                }
                            }
                            Matcher doubleRange = Pattern.compile(rangePatternDouble()).matcher(filter);
                            if (doubleRange.find()) {
                                return Double.valueOf(doubleRange.group(1)) <= value && value <= Double.valueOf(doubleRange.group(3));
                            }
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            case "Boolean":
            case "boolean":
                return instance -> {
                    try {
                        field.setAccessible(true);
                        Object object = field.get(instance);
                        Boolean value = (Boolean) object;
                        if (value != null && (BOOLEAN_TRUE.equalsIgnoreCase(filter) || BOOLEAN_FALSE.equalsIgnoreCase(filter))) {
                            return value.equals(Boolean.valueOf(filter));
                        }
                        return false;
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                        return false;
                    }
                };

            default:
                // Enum
                if (field.getType().isEnum()) {
                    // quoted values needs an exact match
                    if (isQuoted(filter)) {
                        return instance -> {
                            try {
                                Enum enumValue = null;
                                try {
                                    enumValue = Enum.valueOf((Class<Enum>) field.getType(), removeQuotes(filter));
                                } catch (IllegalArgumentException e) {
                                    return false;
                                }
                                field.setAccessible(true);
                                Object object = field.get(instance);
                                Enum value = (Enum) object;
                                if (value != null) {
                                    return value.equals(enumValue);
                                }
                                return false;
                            } catch (IllegalArgumentException | IllegalAccessException e) {
                                log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                                return false;
                            }
                        };
                    }
                    List<Object> enumValues = new ArrayList<>();
                    for (Object enumValue : field.getType().getEnumConstants()) {
                        if (enumValue.toString().toUpperCase().contains(filter.toUpperCase())) {
                            enumValues.add(enumValue);
                            continue;
                        }
                    }
                    return instance -> {
                        try {
                            field.setAccessible(true);
                            Object object = field.get(instance);
                            Enum value = (Enum) object;
                            if (value != null) {
                                return enumValues.contains(value);
                            }
                            return false;
                        } catch (IllegalArgumentException | IllegalAccessException e) {
                            log.error("Can't access {} attribute '{}' on class {}", simpleName, fieldName, clazz.getName(), e);
                            return false;
                        }
                    };
                }
                break;
        }
        return instance -> false;
    }

    private static Map<String, Field> getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> inheritanceClazz = clazz;

        while (inheritanceClazz != null) {
            for (Field field : inheritanceClazz.getDeclaredFields()) {
                // Ignore collections and neither do final nor static fields
                if (!Collection.class.isAssignableFrom(field.getType()) && !Modifier.isFinal(field.getModifiers())
                                && !Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            inheritanceClazz = inheritanceClazz.getSuperclass();
        }
        return fields.stream().collect(Collectors.toMap(field -> field.getName(), field -> field));
    }

    private static String likeValue(String value) {
        return ".*" + value.replace(WILDCARD_MANY, ".*").replace(WILDCARD_ONE, ".{1}").toLowerCase() + ".*";
    }

    private static boolean isQuoted(String value) {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    private static String removeQuotes(String value) {
        return value.replaceAll("^\"|\"$", EMPTY);
    }

    private static List<String> splitOr(String value) {
        return Arrays.asList(value.split(escape(OPERATOR_OR)));
    }

    /**
     * Escapes all RegEx control characters for the usage like in {@link String#replaceAll(String, String)} or {@link String#split(String)}
     * 
     * @param value may containing some RegEx control characters like <code>|</code>, <code>?</code>, or <code>*</code>
     * @return value with escaped RegEx control characters like <code>\\|</code>, <code>\\?</code>, or <code>\\*</code>
     */
    private static String escape(String value) {
        return value.replaceAll("([\\\\\\.\\[\\{\\(\\*\\+\\?\\^\\$\\|])", "\\\\$1");
    }

    private static Long parseDateTimeToMillis(String dateString, boolean end) {
        if (dateString != null) {
            Matcher matcher = Pattern.compile(REGEX_DATE).matcher(dateString);
            if (matcher.find()) {
                if (matcher.group(5) != null) {
                    try {
                        String value = matcher.group(5);
                        if (value.length() > 4) { // from 10k we interpret this value as milliseconds
                            return Long.valueOf(value);
                        }
                        Integer year = Integer.valueOf(value);
                        if (year < 100) { // only two digits of year given
                            year += 2000; // sum it up
                            if (year > WorkhorseUtil.timestamp().getYear()) {
                                year -= 100; // if it is in the future, take it back to the 20th century
                            }
                        }
                        // DD.MM.YYYY
                        if (matcher.group(2) != null && matcher.group(4) != null) {
                            Integer month = Integer.valueOf(matcher.group(4));
                            Integer day = Integer.valueOf(matcher.group(2));
                            LocalDateTime date = LocalDateTime.of(year, month, day, 0, 0, 0).withNano(0);
                            if (end) {

                                return WorkhorseUtil.toEpochMilli(date.plusDays(1).minus(1l, ChronoUnit.MILLIS));
                            }
                            return WorkhorseUtil.toEpochMilli(date);
                        }
                        // MM.YYYY
                        if (matcher.group(2) != null) {
                            Integer month = Integer.valueOf(matcher.group(2));
                            LocalDateTime date = LocalDateTime.of(year, month, 1, 0, 0, 0).withNano(0);
                            if (end) {
                                return WorkhorseUtil.toEpochMilli(date.plusMonths(1).minus(1l, ChronoUnit.MILLIS));
                            }
                            return WorkhorseUtil.toEpochMilli(date);
                        }
                        // YYYY
                        LocalDateTime date = LocalDateTime.of(year, 1, 1, 0, 0, 0).withNano(0);
                        if (end) {
                            return WorkhorseUtil.toEpochMilli(date.plusYears(1).minus(1l, ChronoUnit.MILLIS));
                        }
                        return WorkhorseUtil.toEpochMilli(date);
                    } catch (NumberFormatException | DateTimeException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static boolean validDate(String value) {
        return matches(value, REGEX_DATE);
    }

    private static boolean validLong(String value) {
        return matches(value, REGEX_LONG);
    }

    private static boolean validInt(String value) {
        return matches(value, REGEX_INT);
    }

    private static boolean validShort(String value) {
        return matches(value, REGEX_SHORT);
    }

    private static boolean validFloat(String value) {
        return matches(value, REGEX_FLOAT);
    }

    private static boolean validDouble(String value) {
        return matches(value, REGEX_DOUBLE);
    }

    private static boolean matches(String value, String valueRegex) {
        return value.matches("^" + valueRegex + "$");
    }

    private static String rangePatternDate() {
        return rangePattern(REGEX_DATE);
    }

    private static String rangePatternLong() {
        return rangePattern(REGEX_LONG);
    }

    private static String rangePatternInt() {
        return rangePattern(REGEX_INT);
    }

    private static String rangePatternShort() {
        return rangePattern(REGEX_SHORT);
    }

    private static String rangePatternFloat() {
        return rangePattern(REGEX_FLOAT);
    }

    private static String rangePatternDouble() {
        return rangePattern(REGEX_DOUBLE);
    }

    private static String rangePattern(String valueRegex) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("^(");
        stringBuffer.append(valueRegex);
        stringBuffer.append(")(");
        stringBuffer.append(escape(OPERATOR_TO));
        stringBuffer.append("|");
        stringBuffer.append(escape(OPERATOR_TO_WORD));
        stringBuffer.append(")(");
        stringBuffer.append(valueRegex);
        stringBuffer.append(")$");
        return stringBuffer.toString();
    }

}
