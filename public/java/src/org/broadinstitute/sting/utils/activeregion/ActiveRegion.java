package org.broadinstitute.sting.utils.activeregion;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.broadinstitute.sting.utils.HasGenomeLocation;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: rpoplin
 * Date: 1/4/12
 */

public class ActiveRegion implements HasGenomeLocation {

    private final ArrayList<GATKSAMRecord> reads = new ArrayList<GATKSAMRecord>();
    private final GenomeLoc activeRegionLoc;
    private final GenomeLoc extendedLoc;
    private final int extension;
    private GenomeLoc fullExtentReferenceLoc = null;
    private final GenomeLocParser genomeLocParser;
    public final boolean isActive;

    public ActiveRegion( final GenomeLoc activeRegionLoc, final boolean isActive, final GenomeLocParser genomeLocParser, final int extension ) {
        this.activeRegionLoc = activeRegionLoc;
        this.isActive = isActive;
        this.genomeLocParser = genomeLocParser;
        this.extension = extension;
        extendedLoc = genomeLocParser.createGenomeLoc(activeRegionLoc.getContig(), activeRegionLoc.getStart() - extension, activeRegionLoc.getStop() + extension);
        fullExtentReferenceLoc = extendedLoc;
    }

    // add each read to the bin and extend the reference genome activeRegionLoc if needed
    public void add( final GATKSAMRecord read ) {
        fullExtentReferenceLoc = fullExtentReferenceLoc.union( genomeLocParser.createGenomeLoc( read ) );
        reads.add( read );
    }

    public ArrayList<GATKSAMRecord> getReads() { return reads; }

    public byte[] getReference( final IndexedFastaSequenceFile referenceReader ) {
        return getReference( referenceReader, 0 );
    }

    public byte[] getReference( final IndexedFastaSequenceFile referenceReader, final int padding ) {
       return referenceReader.getSubsequenceAt( fullExtentReferenceLoc.getContig(),
               Math.max(1, fullExtentReferenceLoc.getStart() - padding), 
               Math.min(referenceReader.getSequenceDictionary().getSequence(fullExtentReferenceLoc.getContig()).getSequenceLength(), fullExtentReferenceLoc.getStop() + padding) ).getBases();
    }

    @Override
    public GenomeLoc getLocation() { return activeRegionLoc; }
    public GenomeLoc getExtendedLoc() { return extendedLoc; }
    public GenomeLoc getReferenceLoc() { return fullExtentReferenceLoc; }

    public int getExtension() { return extension; }
    public int size() { return reads.size(); }
    public void clearReads() { reads.clear(); }
    public void remove( final GATKSAMRecord read ) { reads.remove( read ); }
    public void removeAll( final ArrayList<GATKSAMRecord> readsToRemove ) { reads.removeAll( readsToRemove ); }
}