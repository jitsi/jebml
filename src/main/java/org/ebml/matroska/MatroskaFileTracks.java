package org.ebml.matroska;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;

import org.ebml.MasterElement;
import org.ebml.io.DataWriter;
import org.ebml.io.FileDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatroskaFileTracks
{
  final public static String RESIZED = "resized";

  private final PropertyChangeSupport listeners = new PropertyChangeSupport(this);

  private static final long BLOCK_SIZE = 4096;
  private static final Logger LOG = LoggerFactory.getLogger(MatroskaFileTracks.class);

  private final ArrayList<MatroskaFileTrack> tracks = new ArrayList<>();

  private long myStartPosition;
  private long myEndPosition;

  public void addTrack(final MatroskaFileTrack track)
  {
    tracks.add(track);
  }

  public long writeTracks(final DataWriter ioDW)
  {
    myStartPosition = ioDW.getFilePointer();
    final MasterElement tracksElem = MatroskaDocTypes.Tracks.getInstance();

    for (final MatroskaFileTrack track : tracks)
    {
      tracksElem.addChildElement(track.toElement());
    }

    if (BLOCK_SIZE < tracksElem.getTotalSize() && ioDW.isSeekable()
        // do the shuffling the data only if the file is big enough to contain the data
        // if it is not it means we are writing the file for the first time and we don't need to shuffle the data
        && ioDW.length() > myStartPosition + tracksElem.getTotalSize())
    {
      long len;

      // we need to write beyond the void space we have reserved
      // copy beginning of file into a temporary file
      try (FileDataWriter dw = ((FileDataWriter)ioDW).copyBeginningOfFile())
      {
        // write the tracks
        len = tracksElem.writeElement(dw);

        // now let's copy the rest of the original file by first setting the position after the tracks
        ioDW.seek(myEndPosition);

        // copy the rest of the original file
        ((FileDataWriter)ioDW).copyEndOfFile(dw);

        myEndPosition = myStartPosition + len;

        ioDW.seek(myEndPosition);
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      // we need to update tags element that its current position changed as we moved the data and inserted
      // some data before tags
      this.listeners.firePropertyChange(RESIZED, BLOCK_SIZE, len);

      return len;
    }

    long size = tracksElem.writeElement(ioDW);

    myEndPosition = ioDW.getFilePointer();
    if (BLOCK_SIZE > tracksElem.getTotalSize() && ioDW.isSeekable())
    {
      new VoidElement(BLOCK_SIZE - tracksElem.getTotalSize()).writeElement(ioDW);
      return BLOCK_SIZE;
    }

    return size;
  }

  public long update(final DataWriter ioDW)
  {
    LOG.info("Updating tracks list!");
    final long start = ioDW.getFilePointer();
    ioDW.seek(myStartPosition);
    long len = writeTracks(ioDW);
    ioDW.seek(start);
    return len;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener)
  {
    this.listeners.addPropertyChangeListener(listener);
  }
}
