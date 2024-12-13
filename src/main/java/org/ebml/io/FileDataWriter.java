/**
 * JEBML - Java library to read/write EBML/Matroska elements.
 * Copyright (C) 2004 Jory Stone <jebml@jory.info>
 * Based on Javatroska (C) 2002 John Cannon <spyder@matroska.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.ebml.io;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileDataWriter implements DataWriter, Closeable
{
  RandomAccessFile file = null;
  FileChannel fc = null;

  /**
   * We need this in order to be able to replace the file with another one.
   */
  String filename = null;

  public FileDataWriter(final String filename) throws FileNotFoundException, IOException
  {
    file = new RandomAccessFile(filename, "rw");
    fc = file.getChannel();
    this.filename = filename;
  }

  public FileDataWriter(final String filename, final String mode) throws FileNotFoundException, IOException
  {
    file = new RandomAccessFile(filename, mode);
    fc = file.getChannel();
    this.filename = filename;
  }

  @Override
  public int write(final byte b)
  {
    try
    {
      file.write(b);
      return 1;
    }
    catch (final IOException ex)
    {
      return 0;
    }
  }

  @Override
  public int write(final ByteBuffer buff)
  {
    try
    {
      return fc.write(buff);
    }
    catch (final IOException ex)
    {
      return 0;
    }
  }

  @Override
  public long length()
  {
    try
    {
      return file.length();
    }
    catch (final IOException ex)
    {
      return -1;
    }
  }

  @Override
  public long getFilePointer()
  {
    try
    {
      return file.getFilePointer();
    }
    catch (final IOException ex)
    {
      return -1;
    }
  }

  @Override
  public boolean isSeekable()
  {
    return true;
  }

  @Override
  public long seek(final long pos)
  {
    try
    {
      file.seek(pos);
      return file.getFilePointer();
    }
    catch (final IOException ex)
    {
      return -1;
    }
  }

  @Override
  public void close() throws IOException
  {
    file.close();
  }

  /**
   * Copy data from source to source position into current file starting from the beginning.
   */
  public void copyToPosition(FileDataWriter src) throws IOException
  {
    src.fc.transferTo(0, src.fc.position(), fc);
  }

  /**
   * Copy from current position of src to its end to the current file on current position.
   */
    public void copyFromPosition(FileDataWriter src) throws IOException
    {
        src.fc.transferTo(src.fc.position(), src.fc.size() - src.fc.position(), fc);
    }

    public void replaceWithFile(FileDataWriter dw)
            throws IOException
    {
      Files.move(Path.of(dw.filename), Path.of(this.filename), StandardCopyOption.REPLACE_EXISTING);
    }
}
