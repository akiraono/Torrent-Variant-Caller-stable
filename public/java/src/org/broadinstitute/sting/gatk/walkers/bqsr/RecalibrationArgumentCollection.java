/*
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.gatk.walkers.bqsr;

import org.broad.tribble.Feature;
import org.broadinstitute.sting.commandline.*;
import org.broadinstitute.sting.gatk.walkers.recalibration.CountCovariatesGatherer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rpoplin
 * Date: Nov 27, 2009
 *
 * A collection of the arguments that are common to both CovariateCounterWalker and TableRecalibrationWalker.
 * This set of arguments will also be passed to the constructor of every Covariate when it is instantiated.
 */

public class RecalibrationArgumentCollection {

    /**
     * This algorithm treats every reference mismatch as an indication of error. However, real genetic variation is expected to mismatch the reference,
     * so it is critical that a database of known polymorphic sites is given to the tool in order to skip over those sites. This tool accepts any number of RodBindings (VCF, Bed, etc.)
     * for use as this database. For users wishing to exclude an interval list of known variation simply use -XL my.interval.list to skip over processing those sites.
     * Please note however that the statistics reported by the tool will not accurately reflected those sites skipped by the -XL argument.
     */
    @Input(fullName = "knownSites", shortName = "knownSites", doc = "A database of known polymorphic sites to skip over in the recalibration algorithm", required = false)
    protected List<RodBinding<Feature>> knownSites = Collections.emptyList();

    /**
     * After the header, data records occur one per line until the end of the file. The first several items on a line are the
     * values of the individual covariates and will change depending on which covariates were specified at runtime. The last
     * three items are the data- that is, number of observations for this combination of covariates, number of reference mismatches,
     * and the raw empirical quality score calculated by phred-scaling the mismatch rate.
     */
    @Gather(CountCovariatesGatherer.class)
    @Output
    protected PrintStream RECAL_FILE;

    /**
     * List all implemented covariates.
     */
    @Argument(fullName = "list", shortName = "ls", doc = "List the available covariates and exit", required = false)
    protected boolean LIST_ONLY = false;

    /**
     * Covariates to be used in the recalibration. Each covariate is given as a separate cov parameter. ReadGroup and ReportedQuality are required covariates and are already added for you. See the list of covariates with -list.
     */
    @Argument(fullName = "covariate", shortName = "cov", doc = "Covariates to be used in the recalibration. Each covariate is given as a separate cov parameter. ReadGroup and ReportedQuality are required covariates and are already added for you.", required = false)
    protected String[] COVARIATES = null;

    /*
     * Use the standard set of covariates in addition to the ones listed using the -cov argument
     */
    @Argument(fullName = "standard_covs", shortName = "standard", doc = "Use the standard set of covariates in addition to the ones listed using the -cov argument", required = false)
    protected boolean USE_STANDARD_COVARIATES = true;

    /////////////////////////////
    // Debugging-only Arguments
    /////////////////////////////
    /**
     * This calculation is critically dependent on being able to skip over known polymorphic sites. Please be sure that you know what you are doing if you use this option.
     */
    @Hidden
    @Argument(fullName = "run_without_dbsnp_potentially_ruining_quality", shortName = "run_without_dbsnp_potentially_ruining_quality", required = false, doc = "If specified, allows the recalibrator to be used without a dbsnp rod. Very unsafe and for expert users only.")
    protected boolean RUN_WITHOUT_DBSNP = false;

    /////////////////////////////
    // protected Member Variables
    /////////////////////////////
    protected final RecalDataManager dataManager = new RecalDataManager();                // Holds the data HashMap used to create collapsed data hashmaps (delta delta tables)
    protected final ArrayList<Covariate> requestedCovariates = new ArrayList<Covariate>();// A list to hold the covariate objects that were requested

    protected final String SKIP_RECORD_ATTRIBUTE = "SKIP";                                // used to label reads that should be skipped.
    protected final String SEEN_ATTRIBUTE = "SEEN";                                       // used to label reads as processed.


    /**
     * CountCovariates and TableRecalibration accept a --solid_recal_mode <MODE> flag which governs how the recalibrator handles the
     * reads which have had the reference inserted because of color space inconsistencies.
     */
    @Argument(fullName = "solid_recal_mode", shortName = "sMode", required = false, doc = "How should we recalibrate solid bases in which the reference was inserted? Options = DO_NOTHING, SET_Q_ZERO, SET_Q_ZERO_BASE_N, or REMOVE_REF_BIAS")
    public RecalDataManager.SOLID_RECAL_MODE SOLID_RECAL_MODE = RecalDataManager.SOLID_RECAL_MODE.SET_Q_ZERO;

    /**
     * CountCovariates and TableRecalibration accept a --solid_nocall_strategy <MODE> flag which governs how the recalibrator handles
     * no calls in the color space tag. Unfortunately because of the reference inserted bases mentioned above, reads with no calls in
     * their color space tag can not be recalibrated.
     */
    @Argument(fullName = "solid_nocall_strategy", shortName = "solid_nocall_strategy", doc = "Defines the behavior of the recalibrator when it encounters no calls in the color space. Options = THROW_EXCEPTION, LEAVE_READ_UNRECALIBRATED, or PURGE_READ", required = false)
    public RecalDataManager.SOLID_NOCALL_STRATEGY SOLID_NOCALL_STRATEGY = RecalDataManager.SOLID_NOCALL_STRATEGY.THROW_EXCEPTION;

    /**
     * The context covariate will use a context of this size to calculate it's covariate value for base mismatches
     */
    @Argument(fullName = "mismatches_context_size", shortName = "mcs", doc = "size of the k-mer context to be used for base mismatches", required = false)
    public int MISMATCHES_CONTEXT_SIZE = 2;

    /**
     * The context covariate will use a context of this size to calculate it's covariate value for base insertions
     */
    @Argument(fullName = "insertions_context_size", shortName = "ics", doc = "size of the k-mer context to be used for base insertions", required = false)
    public int INSERTIONS_CONTEXT_SIZE = 8;

    /**
     * The context covariate will use a context of this size to calculate it's covariate value for base deletions
     */
    @Argument(fullName = "deletions_context_size", shortName = "dcs", doc = "size of the k-mer context to be used for base deletions", required = false)
    public int DELETIONS_CONTEXT_SIZE = 8;

    /**
     * A default base qualities to use as a prior (reported quality) in the mismatch covariate model. This value will replace all base qualities in the read for this default value. Negative value turns it off (default is off)
     */
    @Argument(fullName = "mismatches_default_quality", shortName = "mdq", doc = "default quality for the base mismatches covariate", required = false)
    public byte MISMATCHES_DEFAULT_QUALITY = -1;

    /**
     * A default base qualities to use as a prior (reported quality) in the insertion covariate model. This parameter is used for all reads without insertion quality scores for each base. (default is on)
     */
    @Argument(fullName = "insertions_default_quality", shortName = "idq", doc = "default quality for the base insertions covariate", required = false)
    public byte INSERTIONS_DEFAULT_QUALITY = 45;

    /**
     * A default base qualities to use as a prior (reported quality) in the mismatch covariate model. This value will replace all base qualities in the read for this default value. Negative value turns it off (default is off)
     */
    @Argument(fullName = "deletions_default_quality", shortName = "ddq", doc = "default quality for the base deletions covariate", required = false)
    public byte DELETIONS_DEFAULT_QUALITY = 45;


    @Hidden
    @Argument(fullName = "default_platform", shortName = "dP", required = false, doc = "If a read has no platform then default to the provided String. Valid options are illumina, 454, and solid.")
    public String DEFAULT_PLATFORM = null;
    @Hidden
    @Argument(fullName = "force_platform", shortName = "fP", required = false, doc = "If provided, the platform of EVERY read will be forced to be the provided String. Valid options are illumina, 454, and solid.")
    public String FORCE_PLATFORM = null;


}
