package org.ebml.matroska;

import java.util.ArrayList;

import org.ebml.MasterElement;
import org.ebml.io.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatroskaFileTracks
{
  private static final int BLOCK_SIZE = 4096;
  private static final Logger LOG = LoggerFactory.getLogger(MatroskaFileTracks.class);

  private final ArrayList<MatroskaFileTrack> tracks = new ArrayList<>();

  private long myPosition;

  public void addTrack(final MatroskaFileTrack track)
  {
    tracks.add(track);
  }

  public long writeTracks(final DataWriter ioDW, boolean checkBlockSize)
    throws VoidOutOfBoundException
  {
    myPosition = ioDW.getFilePointer();
    final MasterElement tracksElem = MatroskaDocTypes.Tracks.getInstance();

    for (final MatroskaFileTrack track : tracks)
    {
      tracksElem.addChildElement(track.toElement());
    }

    if (checkBlockSize && BLOCK_SIZE < tracksElem.getTotalSize())
    {
      LOG.warn("Tracks element size exceeds block size!");

      throw new VoidOutOfBoundException();
    }

    long size = tracksElem.writeElement(ioDW);

    if (BLOCK_SIZE > tracksElem.getTotalSize())
    {
      new VoidElement(BLOCK_SIZE - tracksElem.getTotalSize()).writeElement(ioDW);
      return BLOCK_SIZE;
    }
    else
    {
        return size;
    }
  }

  public long update(final DataWriter ioDW, boolean checkBlockSize)
    throws VoidOutOfBoundException
  {
    LOG.info("Updating tracks list!");
    final long start = ioDW.getFilePointer();
    ioDW.seek(myPosition);
    long len = writeTracks(ioDW, checkBlockSize);
    ioDW.seek(start);
    return len;
  }
}
