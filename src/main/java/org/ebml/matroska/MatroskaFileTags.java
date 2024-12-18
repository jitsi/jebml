package org.ebml.matroska;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;

import org.ebml.MasterElement;
import org.ebml.io.DataWriter;
import org.ebml.io.FileDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatroskaFileTags
  implements PropertyChangeListener
{
  private static final int BLOCK_SIZE = 4096;
  private static final Logger LOG = LoggerFactory.getLogger(MatroskaFileTags.class);

  private final ArrayList<MatroskaFileTagEntry> tags = new ArrayList<>();

  private long myStartPosition;
  private long myEndPosition;

  public void addTag(final MatroskaFileTagEntry tag)
  {
    tags.add(tag);
  }

  public long writeTags(final DataWriter ioDW)
  {
    myStartPosition = ioDW.getFilePointer();
    final MasterElement tagsElem = MatroskaDocTypes.Tags.getInstance();

    for (final MatroskaFileTagEntry tag : tags)
    {
      tagsElem.addChildElement(tag.toElement());
    }

    if (BLOCK_SIZE < tagsElem.getTotalSize() && ioDW.isSeekable()
        // do the shuffling the data only if the file is big enough to contain the data
        // if it is not it means we are writing the file for the first time and we don't need to shuffle the data
        && ioDW.length() > myStartPosition + tagsElem.getTotalSize())
    {
      long len;

      // we need to write beyond the void space we have reserved
      // copy beginning of file into a temporary file
      try (FileDataWriter tmp = ((FileDataWriter)ioDW).copyBeginningOfFile())
      {
        // write the tags
        len = tagsElem.writeElement(tmp);

        // now let's copy the rest of the original file by first setting the position after the tags
        ioDW.seek(myEndPosition);

        // copy the rest of the original file
        ((FileDataWriter)ioDW).copyEndOfFile(tmp);
        myEndPosition = myStartPosition + len;

        ioDW.seek(myEndPosition);
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      return len;
    }

    long len = tagsElem.writeElement(ioDW);
    myEndPosition = ioDW.getFilePointer();
    if (BLOCK_SIZE > tagsElem.getTotalSize() && ioDW.isSeekable())
    {
      new VoidElement(BLOCK_SIZE - tagsElem.getTotalSize()).writeElement(ioDW);
      myEndPosition = ioDW.getFilePointer();
      return BLOCK_SIZE;
    }

    return len;
  }

  public long update(final DataWriter ioDW)
  {
    LOG.info("Updating tags list!");
    final long start = ioDW.getFilePointer();
    ioDW.seek(myStartPosition);
    long len = writeTags(ioDW);
    ioDW.seek(start);
    return len;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    if (evt.getPropertyName().equals(MatroskaFileTracks.RESIZED))
    {
      long increase = (long) evt.getNewValue() - (long) evt.getOldValue();
      myStartPosition += increase;
      myEndPosition += increase;
    }
  }
}
