/**
 * This file is part of VisiCut.
 * Copyright (C) 2011 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * 
 *     VisiCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *    VisiCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 * 
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with VisiCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package com.t_oster.liblasercut.drivers;

import com.t_oster.liblasercut.BlackWhiteRaster;
import com.t_oster.liblasercut.IllegalJobException;
import com.t_oster.liblasercut.LaserCutter;
import com.t_oster.liblasercut.LaserJob;
import com.t_oster.liblasercut.LaserProperty;
import com.t_oster.liblasercut.ProgressListener;
import com.t_oster.liblasercut.Raster3dPart;
import com.t_oster.liblasercut.RasterPart;
import com.t_oster.liblasercut.VectorCommand;
import com.t_oster.liblasercut.VectorPart;
import com.t_oster.liblasercut.platform.Point;
import com.t_oster.liblasercut.platform.Util;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPClient;

/**
 * This class implements a driver for the LAOS Lasercutter board.
 * Currently it supports the simple code and the G-Code, which may be used in
 * the future.
 * 
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class LaosCutter extends LaserCutter
{

  private static final String SETTING_HOSTNAME = "Hostname / IP";
  private static final String SETTING_PORT = "Port";
  private static final String SETTING_BEDWIDTH = "Laserbed width";
  private static final String SETTING_BEDHEIGHT = "Laserbed height";
  private static final String SETTING_FLIPX = "X axis goes right to left (yes/no)";
  private static final String SETTING_MMPERSTEP = "mm per Step (for SimpleMode)";
  private static final String SETTING_TFTP = "Use TFTP instead of TCP";
  private static final String SETTING_RASTER_WHITESPACE = "Additional space per Raster line";
  private static final String SETTING_NATIVERASTER = "Use native (LAOS) rastermode";
  private static final String SETTING_UNIDIR = "Engrave unidirectional";
  
  private boolean unidir = false;
  
  public void setEngraveUnidirectional(boolean uni)
  {
    this.unidir = uni;
  }
  
  public boolean isEngraveUnidirectional()
  {
    return this.unidir;
  }
  
  private boolean useLaosRastermode = false;

  /**
   * Get the value of useLaosRastermode
   *
   * @return the value of useLaosRastermode
   */
  public boolean isUseLaosRastermode()
  {
    return useLaosRastermode;
  }

  /**
   * Set the value of useLaosRastermode
   *
   * @param useLaosRastermode new value of useLaosRastermode
   */
  public void setUseLaosRastermode(boolean useLaosRastermode)
  {
    this.useLaosRastermode = useLaosRastermode;
  }

  
  private double addSpacePerRasterLine = 5;

  /**
   * Get the value of addSpacePerRasterLine
   *
   * @return the value of addSpacePerRasterLine
   */
  public double getAddSpacePerRasterLine()
  {
    return addSpacePerRasterLine;
  }

  /**
   * Set the value of addSpacePerRasterLine
   * This is a space (in mm) for the laserhead to gain
   * speed before the first 'black' pixel in every line
   *
   * @param addSpacePerRasterLine new value of addSpacePerRasterLine
   */
  public void setAddSpacePerRasterLine(double addSpacePerRasterLine)
  {
    this.addSpacePerRasterLine = addSpacePerRasterLine;
  }

  
  @Override
  public String getModelName()
  {
    return "LAOS";
  }
  protected boolean useTftp = true;

  /**
   * Get the value of useTftp
   *
   * @return the value of useTftp
   */
  public boolean isUseTftp()
  {
    return useTftp;
  }

  /**
   * Set the value of useTftp
   *
   * @param useTftp new value of useTftp
   */
  public void setUseTftp(boolean useTftp)
  {
    this.useTftp = useTftp;
  }
  protected boolean flipXaxis = false;

  /**
   * Get the value of flipXaxis
   *
   * @return the value of flipXaxis
   */
  public boolean isFlipXaxis()
  {
    return flipXaxis;
  }

  /**
   * Set the value of flipXaxis
   *
   * @param flipXaxis new value of flipXaxis
   */
  public void setFlipXaxis(boolean flipXaxis)
  {
    this.flipXaxis = flipXaxis;
  }
  
  protected String hostname = "192.168.123.111";

  /**
   * Get the value of hostname
   *
   * @return the value of hostname
   */
  public String getHostname()
  {
    return hostname;
  }

  /**
   * Set the value of hostname
   *
   * @param hostname new value of hostname
   */
  public void setHostname(String hostname)
  {
    this.hostname = hostname;
  }
  protected int port = 69;

  /**
   * Get the value of port
   *
   * @return the value of port
   */
  public int getPort()
  {
    return port;
  }

  /**
   * Set the value of port
   *
   * @param port new value of port
   */
  public void setPort(int port)
  {
    this.port = port;
  }
  protected double mmPerStep = 0.001;

  /**
   * Get the value of mmPerStep
   *
   * @return the value of mmPerStep
   */
  public double getMmPerStep()
  {
    return mmPerStep;
  }

  /**
   * Set the value of mmPerStep
   *
   * @param mmPerStep new value of mmPerStep
   */
  public void setMmPerStep(double mmPerStep)
  {
    this.mmPerStep = mmPerStep;
  }

  private int px2steps(double px, double dpi)
  {
    return (int) (Util.px2mm(px, dpi) / this.mmPerStep);
  }

  private byte[] generateVectorGCode(VectorPart vp, int resolution) throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    int power = 100;
    int speed = 50;
    int frequency = 500;
    double focus = 0;
    //reset saved values, so the first ones get verbosed
    this.currentPower = -1;
    this.currentSpeed = -1;
    this.currentFrequency = -1;
    for (VectorCommand cmd : vp.getCommandList())
    {
      switch (cmd.getType())
      {
        case MOVETO:
          move(out, cmd.getX(), cmd.getY(), resolution);
          break;
        case LINETO:
          line(out, cmd.getX(), cmd.getY(), power, speed, frequency, resolution);
          break;
        case SETPOWER:
          power = cmd.getPower();
          break;
        case SETFOCUS:
          out.printf(Locale.US, "2 %d\n", (int) Util.mm2px(cmd.getFocus(), resolution));
          break;
        case SETSPEED:
          speed = cmd.getSpeed();
          break;
        case SETFREQUENCY:
          frequency = cmd.getFrequency();
          break;
      }
    }
    return result.toByteArray();
  }

  private void move(PrintStream out, int x, int y, int resolution)
  {
    out.printf("0 %d %d\n", px2steps(isFlipXaxis() ? Util.mm2px(bedWidth, resolution) - x : x, resolution), px2steps(y, resolution));
  }
  private int currentPower = -1;
  private int currentSpeed = -1;
  private int currentFrequency = -1;

  private void line(PrintStream out, int x, int y, int power, int speed, int frequency, int resolution)
  {
    if (currentPower != power)
    {
      out.printf("7 101 %d\n", power * 100);
      currentPower = power;
    }
    if (currentSpeed != speed)
    {
      out.printf("7 100 %d\n", speed * 100);
      currentSpeed = speed;
    }
    if (currentFrequency != frequency)
    {
      out.printf("7 102 %d\n", frequency);
      currentFrequency = frequency;
    }
    out.printf("1 %d %d\n", px2steps(isFlipXaxis() ? Util.mm2px(bedWidth, resolution) - x : x, resolution), px2steps(y, resolution));
  }

  private byte[] generatePseudoRaster3dGCode(Raster3dPart rp, int resolution) throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    boolean dirRight = true;
    for (int raster = 0; raster < rp.getRasterCount(); raster++)
    {
      Point rasterStart = rp.getRasterStart(raster);
      LaserProperty prop = rp.getLaserProperty(raster);
      //Set focus
      out.printf(Locale.US, "2 %d\n", (int) Util.mm2px(prop.getFocus(), resolution));
      for (int line = 0; line < rp.getRasterHeight(raster); line++)
      {
        Point lineStart = rasterStart.clone();
        lineStart.y += line;
        List<Byte> bytes = rp.getRasterLine(raster, line);
        //remove heading zeroes
        while (bytes.size() > 0 && bytes.get(0) == 0)
        {
          bytes.remove(0);
          lineStart.x += 1;
        }
        //remove trailing zeroes
        while (bytes.size() > 0 && bytes.get(bytes.size() - 1) == 0)
        {
          bytes.remove(bytes.size() - 1);
        }
        if (bytes.size() > 0)
        {
          if (dirRight)
          {
            //move to the first nonempyt point of the line
            move(out, lineStart.x, lineStart.y, resolution);
            byte old = bytes.get(0);
            for (int pix = 0; pix < bytes.size(); pix++)
            {
              if (bytes.get(pix) != old)
              {
                if (old == 0)
                {
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                else
                {
                  line(out, lineStart.x + pix - 1, lineStart.y, prop.getPower() * (0xFF & old) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                old = bytes.get(pix);
              }
            }
            //last point is also not "white"
            line(out, lineStart.x + bytes.size() - 1, lineStart.y, prop.getPower() * (0xFF & bytes.get(bytes.size() - 1)) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
          }
          else
          {
            //move to the last nonempty point of the line
            move(out, lineStart.x + bytes.size() - 1, lineStart.y, resolution);
            byte old = bytes.get(bytes.size() - 1);
            for (int pix = bytes.size() - 1; pix >= 0; pix--)
            {
              if (bytes.get(pix) != old || pix == 0)
              {
                if (old == 0)
                {
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                else
                {
                  line(out, lineStart.x + pix + 1, lineStart.y, prop.getPower() * (0xFF & old) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                old = bytes.get(pix);
              }
            }
            //last point is also not "white"
            line(out, lineStart.x, lineStart.y, prop.getPower() * (0xFF & bytes.get(0)) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
          }
        }
        if (!this.isEngraveUnidirectional())
        {
          dirRight = !dirRight;
        }
      }
    }
    return result.toByteArray();
  }

  private byte[] generatePseudoRasterGCode(RasterPart rp, int resolution) throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    boolean dirRight = true;
    for (int raster = 0; raster < rp.getRasterCount(); raster++)
    {
      Point rasterStart = rp.getRasterStart(raster);
      LaserProperty prop = rp.getLaserProperty(raster);
      //Set focus
      out.printf(Locale.US, "2 %d\n", (int) Util.mm2px(prop.getFocus(), resolution));
      for (int line = 0; line < rp.getRasterHeight(raster); line++)
      {
        Point lineStart = rasterStart.clone();
        lineStart.y += line;
        //Convert BlackWhite line into line of 0 and 255 bytes
        BlackWhiteRaster bwr = rp.getImages()[raster];
        List<Byte> bytes = new LinkedList<Byte>();
        boolean lookForStart = true;
        for (int x = 0; x < bwr.getWidth(); x++)
        {
          if (lookForStart)
          {
            if (bwr.isBlack(x, line))
            {
              lookForStart = false;
              bytes.add((byte) 255);
            }
            else
            {
              lineStart.x += 1;
            }
          }
          else
          {
            bytes.add(bwr.isBlack(x, line) ? (byte) 255 : (byte) 0);
          }
        }
        //remove trailing zeroes
        while (bytes.size() > 0 && bytes.get(bytes.size() - 1) == 0)
        {
          bytes.remove(bytes.size() - 1);
        }
        if (bytes.size() > 0)
        {
          if (dirRight)
          {
            //add some space to the left
            move(out, Math.max(0, (int) (lineStart.x-Util.mm2px(this.addSpacePerRasterLine, resolution))), lineStart.y, resolution);
            //move to the first nonempyt point of the line
            move(out, lineStart.x, lineStart.y, resolution);
            byte old = bytes.get(0);
            for (int pix = 0; pix < bytes.size(); pix++)
            {
              if (bytes.get(pix) != old)
              {
                if (old == 0)
                {
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                else
                {
                  line(out, lineStart.x + pix - 1, lineStart.y, prop.getPower() * (0xFF & old) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                old = bytes.get(pix);
              }
            }
            //last point is also not "white"
            line(out, lineStart.x + bytes.size() - 1, lineStart.y, prop.getPower() * (0xFF & bytes.get(bytes.size() - 1)) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
            //add some space to the right
            move(out, Math.min((int) Util.mm2px(bedWidth, resolution), (int) (lineStart.x + bytes.size() - 1 + Util.mm2px(this.addSpacePerRasterLine, resolution))), lineStart.y, resolution);
          }
          else
          {
            //add some space to the right
            move(out, Math.min((int) Util.mm2px(bedWidth, resolution), (int) (lineStart.x + bytes.size() - 1 + Util.mm2px(this.addSpacePerRasterLine, resolution))), lineStart.y, resolution);
            //move to the last nonempty point of the line
            move(out, lineStart.x + bytes.size() - 1, lineStart.y, resolution);
            byte old = bytes.get(bytes.size() - 1);
            for (int pix = bytes.size() - 1; pix >= 0; pix--)
            {
              if (bytes.get(pix) != old || pix == 0)
              {
                if (old == 0)
                {
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                else
                {
                  line(out, lineStart.x + pix + 1, lineStart.y, prop.getPower() * (0xFF & old) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
                  move(out, lineStart.x + pix, lineStart.y, resolution);
                }
                old = bytes.get(pix);
              }
            }
            //last point is also not "white"
            line(out, lineStart.x, lineStart.y, prop.getPower() * (0xFF & bytes.get(0)) / 255, prop.getSpeed(), prop.getFrequency(), resolution);
            //add some space to the left
            move(out, Math.max(0, (int) (lineStart.x-Util.mm2px(this.addSpacePerRasterLine, resolution))), lineStart.y, resolution);
          }
        }
        if (!this.isEngraveUnidirectional())
        {
          dirRight = !dirRight;
        }
      }
    }
    return result.toByteArray();
  }

  /**
   * This Method takes a raster-line represented by a list of bytes, 
   * where: byte0 ist the left-most byte, in one byte, the LSB is the
   * left-most bit, 0 representing laser off, 1 representing laser on.
   * The Output List of longs, where each value is the unsigned dword
   * of 4 bytes of the input each, where the first dword is the leftmost
   * dword and the LSB is the leftmost bit. If outputLeftToRight is false,
   * the first dword is the rightmost dword and the LSB of each dword is the
   * the Output is padded with zeroes on the right side, if leftToRight is true,
   * on the left-side otherwise
   * rightmost bit
   * @param line
   * @param outputLeftToRight
   * @return 
   */
  private List<Long> byteLineToDwords(List<Byte> line, boolean outputLeftToRight)
  {
    List<Long> result = new ArrayList<Long>();
    int s = line.size();
    for(int i=0; i<s; i+=4)
    {
      result.add(
        (((long) (i+3 < s ? line.get(i+3) : 0))<<24) 
        + (((long) (i+2 < s ? line.get(i+2) : 0))<<16)
        + (((long) (i+1 < s ? line.get(i+1) : 0))<<8)
        + line.get(i)
        );
    }
    if (!outputLeftToRight)
    {
      Collections.reverse(result);
      for(int i=0;i<result.size();i++)
      {
        result.set(i, Long.reverse(result.get(i)));
      }
    }
    return result;
  }
  
  private byte[] generateLaosRasterCode(RasterPart rp, int resolution) throws UnsupportedEncodingException, IOException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    boolean dirRight = true;
    for (int raster = 0; raster < rp.getRasterCount(); raster++)
    {
      Point rasterStart = rp.getRasterStart(raster);
      LaserProperty prop = rp.getLaserProperty(raster);
      //Set focus
      out.printf(Locale.US, "2 %d\n", (int) Util.mm2px(prop.getFocus(), resolution));
      for (int line = 0; line < rp.getRasterHeight(raster); line++)
      {
        Point lineStart = rasterStart.clone();
        lineStart.y += line;
        List<Byte> bytes = rp.getRasterLine(raster, line);
        //remove heading zeroes
        while (bytes.size() > 0 && bytes.get(0) == 0)
        {
          lineStart.x += 8;
          bytes.remove(0);
        }
        //remove trailing zeroes
        while (bytes.size() > 0 && bytes.get(bytes.size()-1) == 0)
        {
          bytes.remove(bytes.size()-1);
        }
        if (bytes.size() > 0)
        {
          //add space on the left side
          int space = (int) Util.mm2px(this.getAddSpacePerRasterLine(), resolution);
          while (space > 0 && lineStart.x >= 8)
          {
            bytes.add(0, (byte) 0);
            space -= 8;
            lineStart.x -=8;
          }
          //add space on the right side
          space = (int) Util.mm2px(this.getAddSpacePerRasterLine(), resolution);
          int max = (int) Util.mm2px(this.getBedWidth(), resolution);
          while (space > 0 && lineStart.x+(8*bytes.size()) < max-8)
          {
            bytes.add((byte) 0);
            space -= 8;
          }    
          if (dirRight)
          {
            //move to the first point of the line
            move(out, lineStart.x, lineStart.y, resolution);         
            List<Long> dwords = this.byteLineToDwords(bytes, true);
            out.printf("9 %s %s ", "1", ""+(dwords.size()*32));
            for(Long d:dwords)
            {
              out.print(" "+d);
            }
            out.printf("\n");
            line(out, lineStart.x + (dwords.size()*32), lineStart.y, prop.getPower(), prop.getSpeed(), prop.getFrequency(), resolution);
          }
          else
          {
            //move to the first point of the line
            List<Long> dwords = this.byteLineToDwords(bytes, false);
            move(out, lineStart.x+(dwords.size()*32), lineStart.y, resolution);         
            out.printf("9 %s %s ", "1", ""+(dwords.size()*32));
            for(Long d:dwords)
            {
              out.printf(" "+d);
            }
            out.printf("\n");
            line(out, lineStart.x, lineStart.y, prop.getPower(), prop.getSpeed(), prop.getFrequency(), resolution);  
          }
        }
        if (!this.isEngraveUnidirectional())
        {
          dirRight = !dirRight;
        }
      }
    }
    return result.toByteArray();
  }
  
  private byte[] generateInitializationCode() throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    return result.toByteArray();
  }

  private byte[] generateShutdownCode() throws UnsupportedEncodingException
  {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(result, true, "US-ASCII");
    //back to origin and shutdown
    //out.printf("0 0 0\n");
    //Set focus to 0
    out.printf(Locale.US, "2 %d\n", 0);
    return result.toByteArray();
  }

  @Override
  public void sendJob(LaserJob job, ProgressListener pl) throws IllegalJobException, Exception
  {
    pl.progressChanged(this, 0);
    this.currentFrequency = -1;
    this.currentPower = -1;
    this.currentSpeed = -1;
    BufferedOutputStream out;
    ByteArrayOutputStream buffer = null;
    pl.taskChanged(this, "checking job");
    checkJob(job);
    if (!useTftp)
    {
      pl.taskChanged(this, "connecting");
      Socket connection = new Socket();
      connection.connect(new InetSocketAddress(hostname, port), 3000);
      out = new BufferedOutputStream(connection.getOutputStream());
      pl.taskChanged(this, "sending");
    }
    else
    {
      buffer = new ByteArrayOutputStream();
      out = new BufferedOutputStream(buffer);
      pl.taskChanged(this, "buffering");
    }
    out.write(this.generateInitializationCode());
    pl.progressChanged(this, 20);
    if (job.contains3dRaster())
    {
      out.write(this.generatePseudoRaster3dGCode(job.getRaster3dPart(), job.getResolution()));
    }
    pl.progressChanged(this, 40);
    if (job.containsRaster())
    {
      if (this.isUseLaosRastermode())
      {
        out.write(this.generateLaosRasterCode(job.getRasterPart(), job.getResolution()));
      }
      else
      {
        out.write(this.generatePseudoRasterGCode(job.getRasterPart(), job.getResolution()));
      }
    }
    pl.progressChanged(this, 60);
    if (job.containsVector())
    {
      out.write(this.generateVectorGCode(job.getVectorPart(), job.getResolution()));
    }
    pl.progressChanged(this, 80);
    out.write(this.generateShutdownCode());
    out.close();
    if (this.isUseTftp())
    {
      pl.taskChanged(this, "connecting");
      TFTPClient tftp = new TFTPClient();
      tftp.setDefaultTimeout(5000);
      //open a local UDP socket
      tftp.open();
      pl.taskChanged(this, "sending");
      ByteArrayInputStream bain = new ByteArrayInputStream(buffer.toByteArray());
      tftp.sendFile(job.getName().replace(" ", "") +".lgc", TFTP.BINARY_MODE, bain, this.getHostname(), this.getPort());
      tftp.close();
      bain.close();
      pl.taskChanged(this, "sent.");
    }
    pl.progressChanged(this, 100);
  }
  private List<Integer> resolutions;

  @Override
  public List<Integer> getResolutions()
  {
    if (resolutions == null)
    {
      //TODO: Calculate possible resolutions
      //according to mm/step
      resolutions = Arrays.asList(new Integer[]
        {
          500
        });
    }
    return resolutions;
  }
  protected double bedWidth = 250;

  /**
   * Get the value of bedWidth
   *
   * @return the value of bedWidth
   */
  @Override
  public double getBedWidth()
  {
    return bedWidth;
  }

  /**
   * Set the value of bedWidth
   *
   * @param bedWidth new value of bedWidth
   */
  public void setBedWidth(double bedWidth)
  {
    this.bedWidth = bedWidth;
  }
  protected double bedHeight = 280;

  /**
   * Get the value of bedHeight
   *
   * @return the value of bedHeight
   */
  @Override
  public double getBedHeight()
  {
    return bedHeight;
  }

  /**
   * Set the value of bedHeight
   *
   * @param bedHeight new value of bedHeight
   */
  public void setBedHeight(double bedHeight)
  {
    this.bedHeight = bedHeight;
  }
  private List<String> settingAttributes;

  @Override
  public List<String> getSettingAttributes()
  {
    if (settingAttributes == null)
    {
      settingAttributes = new LinkedList<String>();
      settingAttributes.add(SETTING_HOSTNAME);
      settingAttributes.add(SETTING_PORT);
      settingAttributes.add(SETTING_NATIVERASTER);
      settingAttributes.add(SETTING_UNIDIR);
      settingAttributes.add(SETTING_BEDWIDTH);
      settingAttributes.add(SETTING_BEDHEIGHT);
      settingAttributes.add(SETTING_FLIPX);
      settingAttributes.add(SETTING_MMPERSTEP);
      settingAttributes.add(SETTING_TFTP);
      settingAttributes.add(SETTING_RASTER_WHITESPACE);
    }
    return settingAttributes;
  }

  @Override
  public String getSettingValue(String attribute)
  {
    if (SETTING_RASTER_WHITESPACE.equals(attribute))
    {
      return "" + this.getAddSpacePerRasterLine();
    }
    else if (SETTING_NATIVERASTER.equals(attribute))
    {
      return this.isUseLaosRastermode() ? "yes" : "no";
    }
    else if (SETTING_UNIDIR.equals(attribute))
    {
      return this.isEngraveUnidirectional() ? "yes" : "no";
    }
    else if (SETTING_HOSTNAME.equals(attribute))
    {
      return this.getHostname();
    }
    else if (SETTING_FLIPX.equals(attribute))
    {
      return this.isFlipXaxis() ? "yes" : "no";
    }
    else if (SETTING_PORT.equals(attribute))
    {
      return "" + this.getPort();
    }
    else if (SETTING_BEDWIDTH.equals(attribute))
    {
      return "" + this.getBedWidth();
    }
    else if (SETTING_BEDHEIGHT.equals(attribute))
    {
      return "" + this.getBedHeight();
    }
    else if (SETTING_MMPERSTEP.equals(attribute))
    {
      return "" + this.getMmPerStep();
    }
    else if (SETTING_TFTP.equals(attribute))
    {
      return this.isUseTftp() ? "yes" : "no";
    }
    return null;
  }

  @Override
  public void setSettingValue(String attribute, String value)
  {
    if (SETTING_RASTER_WHITESPACE.equals(attribute))
    {
      this.setAddSpacePerRasterLine(Double.parseDouble(value));
    }
    else if (SETTING_NATIVERASTER.equals(attribute))
    {
      this.setUseLaosRastermode("yes".equals(value));
    }
    else if (SETTING_UNIDIR.endsWith(attribute))
    {
      this.setEngraveUnidirectional("yes".equals(value));
    }
    else if (SETTING_HOSTNAME.equals(attribute))
    {
      this.setHostname(value);
    }
    else if (SETTING_PORT.equals(attribute))
    {
      this.setPort(Integer.parseInt(value));
    }
    else if (SETTING_FLIPX.equals(attribute))
    {
      this.setFlipXaxis("yes".equals(value));
    }
    else if (SETTING_BEDWIDTH.equals(attribute))
    {
      this.setBedWidth(Double.parseDouble(value));
    }
    else if (SETTING_BEDHEIGHT.equals(attribute))
    {
      this.setBedHeight(Double.parseDouble(value));
    }
    else if (SETTING_MMPERSTEP.equals(attribute))
    {
      this.setMmPerStep(Double.parseDouble(value));
    }
    else if (SETTING_TFTP.contains(attribute))
    {
      this.setUseTftp("yes".equals(value));
    }
  }

  @Override
  public int estimateJobDuration(LaserJob job)
  {
    return 10000;
  }

  @Override
  public LaserCutter clone()
  {
    LaosCutter clone = new LaosCutter();
    clone.hostname = hostname;
    clone.port = port;
    clone.bedHeight = bedHeight;
    clone.bedWidth = bedWidth;
    clone.flipXaxis = flipXaxis;
    clone.mmPerStep = mmPerStep;
    clone.useTftp = useTftp;
    clone.addSpacePerRasterLine = addSpacePerRasterLine;
    clone.unidir = unidir;
    clone.useLaosRastermode = useLaosRastermode;
    return clone;
  }
}
