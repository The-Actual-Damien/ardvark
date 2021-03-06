/*
 * This file is part of Arduino.
 *
 * Copyright 2019 Arduino LLC (http://www.arduino.cc/)
 *
 * Arduino is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, you may use this file as part of a free software
 * library without restriction.  Specifically, if other files instantiate
 * templates or use macros or inline functions from this file, or you compile
 * this file and link it with other files to produce an executable, this
 * file does not by itself cause the resulting executable to be covered by
 * the GNU General Public License.  This exception does not however
 * invalidate any other reasons why the executable file might be covered by
 * the GNU General Public License.
 */

package cc.arduino.cli;

import java.io.IOException;
import java.util.Iterator;

import cc.arduino.cli.commands.ArduinoCoreGrpc;
import cc.arduino.cli.commands.ArduinoCoreGrpc.ArduinoCoreBlockingStub;
import cc.arduino.cli.commands.Commands.InitReq;
import cc.arduino.cli.commands.Commands.InitResp;
import cc.arduino.cli.commands.Common.Instance;
import cc.arduino.cli.settings.SettingsGrpc;
import cc.arduino.cli.settings.SettingsGrpc.SettingsBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusException;
import processing.app.BaseNoGui;
import processing.app.debug.MessageSiphon;
import processing.app.helpers.ProcessUtils;

public class ArduinoCore {

  private Process cliProcess;
  private ArduinoCoreBlockingStub coreBlocking;
  private SettingsBlockingStub settingsBlocking;
  // private ArduinoCoreStub async;

  public ArduinoCore() throws IOException {
    String cliPath = BaseNoGui.getContentFile("arduino-cli").getAbsolutePath();
    cliProcess = ProcessUtils.exec(new String[] { cliPath, "daemon" });
    new MessageSiphon(cliProcess.getInputStream(), (msg) -> {
      System.out.println("CLI> " + msg);
    });
    new MessageSiphon(cliProcess.getErrorStream(), (msg) -> {
      System.err.println("CLI> " + msg);
    });

    // TODO: Do a better job managing the arduino-cli process
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
    }
    if (!cliProcess.isAlive()) {
      int res;
      try {
        res = cliProcess.waitFor();
        throw new IOException(
            "Arduino server terminated with return code " + res);
      } catch (InterruptedException e) {
      }
      throw new IOException("Arduino server terminated");
    }

    ManagedChannel channel = ManagedChannelBuilder //
        .forAddress("127.0.0.1", 50051) //
        .usePlaintext() //
        .maxInboundMessageSize(Integer.MAX_VALUE) //
        .build();
    coreBlocking = ArduinoCoreGrpc.newBlockingStub(channel);
    settingsBlocking = SettingsGrpc.newBlockingStub(channel);
    // async = ArduinoCoreGrpc.newStub(channel);
  }

  public ArduinoCoreInstance init() throws StatusException {
    Iterator<InitResp> resp = coreBlocking.init(InitReq.getDefaultInstance());
    Instance instance = null;
    while (resp.hasNext()) {
      InitResp r = resp.next();
      if (r.hasTaskProgress()) {
        System.out.println(r.getTaskProgress());
      }
      if (r.hasDownloadProgress()) {
        System.out.println(r.getDownloadProgress());
      }
      if (r.getInstance() != null) {
        if (!r.getLibrariesIndexError().isEmpty()) {
          System.err.println(r.getLibrariesIndexError());
        }
        r.getPlatformsIndexErrorsList().forEach(System.err::println);
        instance = r.getInstance();
      }
    }
    ArduinoCoreInstance core = new ArduinoCoreInstance(instance, coreBlocking, settingsBlocking);
    core.updateSettingFromPreferences();
    return core;
  }
}
