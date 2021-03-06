package com.wilqor.workshop.bytebay.lucene.analysis;

import com.wilqor.workshop.bytebay.lucene.BaseReadingTest;
import com.wilqor.workshop.bytebay.lucene.config.ConfigLoader;
import com.wilqor.workshop.bytebay.lucene.config.IndexType;
import com.wilqor.workshop.bytebay.lucene.source.model.CommentedReviewWithTimestamp;
import com.wilqor.workshop.bytebay.lucene.source.model.SimpleReview;
import com.wilqor.workshop.bytebay.lucene.source.model.Thumb;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.LongRangeFacetCounts;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FacetingExampleTest extends BaseReadingTest {
    private static final int TOP_SORTED_SET_DOC_VALUES_FACETS = 3;
    private static final int TOP_N_DOCUMENT_HITS = 0;

    private SortedSetDocValuesReaderState state;

    @Override
    protected Path provideDirectoryPath() {
        return ConfigLoader.LOADER.getPathForIndex(IndexType.FACETING_EXAMPLE);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        state = new DefaultSortedSetDocValuesReaderState(searcher.getIndexReader());
    }

    @Test
    public void shouldFindMostCommentedArticle() throws Exception {
        FacetsCollector fc = collectFacets(new MatchAllDocsQuery());
        FacetResult topArticlesResult = getSortedSetDocValuesFacetResult(fc, SimpleReview.ARTICLE_NAME_FIELD);

        LabelAndValue[] labelValues = topArticlesResult.labelValues;
        LabelAndValue topArticle = labelValues[0];
        assertThat(topArticle.label, is("Lucene 101"));
        assertThat(topArticle.value, is(2));
    }

    @Test
    public void shouldFindMostActiveUser() throws Exception {
        FacetsCollector fc = collectFacets(new MatchAllDocsQuery());
        FacetResult topUsersResult = getSortedSetDocValuesFacetResult(fc, SimpleReview.USER_NAME_FIELD);

        LabelAndValue[] labelValues = topUsersResult.labelValues;
        LabelAndValue topUser = labelValues[0];
        assertThat(topUser.label, is("zbyszkop"));
        assertThat(topUser.value, is(3));
    }

    @Test
    public void shouldCountThumbsForArticle() throws Exception {
        TermQuery articleFilterQuery = new TermQuery(new Term(SimpleReview.ARTICLE_NAME_FIELD, "Lucene best practices"));
        FacetsCollector fc = collectFacets(articleFilterQuery);
        FacetResult topThumbsResult = getSortedSetDocValuesFacetResult(fc, SimpleReview.THUMB_FIELD);

        LabelAndValue[] labelValues = topThumbsResult.labelValues;
        assertThat(labelValues, is(arrayWithSize(1)));

        LabelAndValue topThumbs = labelValues[0];
        assertThat(topThumbs.label, is(Thumb.UP.name()));
        assertThat(topThumbs.value, is(2));
    }

    @Test
    public void shouldGroupByTimestamp() throws Exception {
        long lowest = 1520422703;
        long highest = 1520422743;
        int noOfGroups = 5;

        LongRange[] ranges = getRanges(lowest, highest, noOfGroups);

        FacetsCollector fc = collectFacets(new MatchAllDocsQuery());
        FacetResult result = getLongRangeFacetResult(fc, CommentedReviewWithTimestamp.TIMESTAMP_FIELD, ranges);

        assertThat(result.childCount, is(noOfGroups));
        Arrays.stream(result.labelValues)
                .forEach(labelAndValue -> assertThat(labelAndValue.value, is(1)));
    }

    private FacetsCollector collectFacets(Query query) throws IOException {
        FacetsCollector facetsCollector = new FacetsCollector();
        FacetsCollector.search(searcher, query, TOP_N_DOCUMENT_HITS, facetsCollector);
        return facetsCollector;
    }

    private FacetResult getLongRangeFacetResult(FacetsCollector fc, String facetFieldName, LongRange[] ranges) throws IOException {
        LongRangeFacetCounts facets = new LongRangeFacetCounts(facetFieldName, fc, ranges);
        return facets.getTopChildren(TOP_N_DOCUMENT_HITS, facetFieldName);
    }

    private FacetResult getSortedSetDocValuesFacetResult(FacetsCollector fc, String facetFieldName) throws IOException {
        Facets facets = new SortedSetDocValuesFacetCounts(state, fc);
        return facets.getTopChildren(TOP_SORTED_SET_DOC_VALUES_FACETS, facetFieldName);
    }

    private LongRange[] getRanges(long lowest, long highest, int noOfGroups) {
        long rangeSize = (highest - lowest) / noOfGroups;
        return IntStream.range(0, noOfGroups)
                .mapToObj(rangeId -> {
                    long rangeStart = lowest + rangeId * rangeSize;
                    long rangeEnd = rangeStart + rangeSize;
                    boolean shouldHaveInclusiveEnd = rangeId == noOfGroups - 1;
                    return new LongRange(rangeStart + "-" + rangeEnd, rangeStart, true, rangeEnd, shouldHaveInclusiveEnd);
                }).toArray(LongRange[]::new);
    }
}