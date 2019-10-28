package org.ebml.matroska;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import org.ebml.io.FileDataSource;
import org.ebml.io.FileDataWriter;
import org.ebml.matroska.MatroskaFileTrack.MatroskaVideoTrack;
import org.ebml.matroska.MatroskaFileTrack.TrackType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatroskaFileReadTest
{
  private static final Logger LOG = LoggerFactory.getLogger(MatroskaFileReadTest.class);
  private static File destination;
  private static MatroskaFileTrack testTrack;
  private static MatroskaFileTagEntry testTag;
  private MatroskaFile file;

  @BeforeClass
  public static void setUpClass() throws Exception
  {
    destination = File.createTempFile("test", ".mkv");
    try (FileDataWriter ioDW = new FileDataWriter(destination.getPath());
         MatroskaFileWriter writer = new MatroskaFileWriter(ioDW))
    {
      testTrack = new MatroskaFileTrack();
      testTrack.setTrackNo(42);
      testTrack.setTrackType(TrackType.VIDEO);
      testTrack.setCodecID("V_MPEG4/ISO/AVC");
      testTrack.setDefaultDuration(33);
      MatroskaVideoTrack video = new MatroskaVideoTrack();
      video.setDisplayHeight((short) 1080);
      video.setDisplayWidth((short) 1920);
      testTrack.setVideo(video);
      writer.addTrack(testTrack);
      MatroskaFileSimpleTag simpleTag = new MatroskaFileSimpleTag();
      simpleTag.setName("TITLE");
      simpleTag.setValue("Canon in D");
      testTag = new MatroskaFileTagEntry();
      testTag.addSimpleTag(simpleTag);
      writer.addTag(testTag);
      for (int i = 0; i < 100; ++i)
      {
        MatroskaFileFrame frame = new MatroskaFileFrame();
        frame.setData(ByteBuffer.wrap(new byte[] {(byte) i }));
        frame.setTimecode(i * 5);
        frame.setTrackNo(42);
        frame.setKeyFrame(i % 3 == 0);
        writer.addFrame(frame);
      }
    }
  }

  @Before
  public void setUp() throws Exception
  {
    FileDataSource ioDS = new FileDataSource(destination.getPath());
    file = new MatroskaFile(ioDS);
    file.readFile();
  }

  @Test
  public void testGetTags()
  {
    List<MatroskaFileTagEntry> tagEntries = file.getTagList();
    Assert.assertEquals(1, tagEntries.size());
    List<MatroskaFileSimpleTag> tags = tagEntries.get(0).simpleTags;
    Assert.assertEquals(1, tags.size());
    MatroskaFileSimpleTag tag = tags.get(0);
    Assert.assertEquals("TITLE", tag.getName());
    Assert.assertEquals("Canon in D", tag.getValue());
  }

  @Test
  public void testSeek()
  {
    LOG.debug("Testing seek");
    long seeked = file.seek(123);
    MatroskaFileFrame nextFrame = file.getNextFrame();
    Assert.assertEquals(120, seeked);
    Assert.assertEquals(120, nextFrame.getTimecode());
  }

  @Test
  public void testPlayThrough()
  {
    for (int i = 0; i < 100; ++i)
    {
      MatroskaFileFrame frame = file.getNextFrame();
      Assert.assertEquals(i, frame.getData().get());
      Assert.assertEquals(i * 5, frame.getTimecode());
      Assert.assertEquals(42, frame.getTrackNo());
      Assert.assertEquals(i % 3 == 0, frame.isKeyFrame());
    }
  }

  @Test
  public void testPlayBackwards()
  {
    file.seek(500);
    for (int i = 99; i >= 0; --i)
    {
      MatroskaFileFrame frame = file.getPreviousFrame();
      Assert.assertEquals(i, frame.getData().get());
      Assert.assertEquals(i * 5, frame.getTimecode());
      Assert.assertEquals(42, frame.getTrackNo());
      Assert.assertEquals(i % 3 == 0, frame.isKeyFrame());
    }
  }
}
