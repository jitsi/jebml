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

  private long myPosition;

  public void addTag(final MatroskaFileTagEntry tag)
  {
    tags.add(tag);
  }

  public long writeTags(final DataWriter ioDW)
  {
    myPosition = ioDW.getFilePointer();
    final MasterElement tagsElem = MatroskaDocTypes.Tags.getInstance();

    for (final MatroskaFileTagEntry tag : tags)
    {
      tagsElem.addChildElement(tag.toElement());
    }

    if (BLOCK_SIZE < tagsElem.getTotalSize() && ioDW.isSeekable())
    {
      long len;

      // we need to write beyond the void space we have reserved
      // copy beginning of file into a temporary file
      try (FileDataWriter dw = ((FileDataWriter)ioDW).copyBeginningOfFile())
      {
        // write the tags
        len = tagsElem.writeElement(dw);

        // now let's copy the rest of the original file by first setting the position after the tags
        ioDW.seek(myPosition + BLOCK_SIZE);

        // copy the rest of the original file
        ((FileDataWriter)ioDW).copyEndOfFile(dw);
      }
      catch (IOException ex)
      {
        throw new RuntimeException(ex);
      }

      return len;
    }

    long len = tagsElem.writeElement(ioDW);

    if (BLOCK_SIZE > tagsElem.getTotalSize() && ioDW.isSeekable())
    {
      new VoidElement(BLOCK_SIZE - tagsElem.getTotalSize()).writeElement(ioDW);
      return BLOCK_SIZE;
    }

    return len;
  }

  public long update(final DataWriter ioDW)
  {
    LOG.info("Updating tags list!");
    final long start = ioDW.getFilePointer();
    ioDW.seek(myPosition);
    long len = writeTags(ioDW);
    ioDW.seek(start);
    return len;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt)
  {
    if (evt.getPropertyName().equals(MatroskaFileTracks.RESIZED))
    {
      myPosition = myPosition + ((long) evt.getNewValue() - (long) evt.getOldValue());
    }
  }
}
