/*
 * This file is part of Arduino.
 *
 * Copyright 2015 Arduino LLC (http://www.arduino.cc/)
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

package cc.arduino.contributions.libraries.ui;

import static processing.app.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.JComboBox;
import javax.swing.JTable;

import cc.arduino.contributions.DownloadableContributionVersionComparator;
import cc.arduino.contributions.VersionComparator;
import cc.arduino.contributions.libraries.ContributedLibrary;
import cc.arduino.contributions.libraries.ContributedLibraryRelease;
import cc.arduino.contributions.ui.InstallerTableCell;
import cc.arduino.utils.ReverseComparator;

@SuppressWarnings("serial")
public class ContributedLibraryTableCellEditor extends InstallerTableCell {

  private ContributedLibrary editorLibrary;
  private ContributedLibraryTableCellJPanel editorCell;

  @Override
  public Object getCellEditorValue() {
    return editorLibrary;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value,
                                               boolean isSelected, int row,
                                               int column) {
    editorLibrary = (ContributedLibrary) value;

    editorCell = new ContributedLibraryTableCellJPanel(table, value, true);
    editorCell.installButton
        .addActionListener(e -> onInstall(editorLibrary.getSelected().get(),
                                          editorLibrary.getInstalled()));
    editorCell.downgradeButton.addActionListener(e -> {
      JComboBox chooser = editorCell.downgradeChooser;
      ContributedLibraryRelease lib = (ContributedLibraryRelease) chooser.getSelectedItem();
      onInstall(lib, editorLibrary.getInstalled());
    });
    editorCell.versionToInstallChooser.addActionListener(e -> {
      editorLibrary.select((ContributedLibraryRelease) editorCell.versionToInstallChooser.getSelectedItem());
      if (editorCell.versionToInstallChooser.getSelectedIndex() != 0) {
        InstallerTableCell.dropdownSelected(true);
      }
    });

    setEnabled(true);

    final Optional<ContributedLibraryRelease> mayInstalled = editorLibrary.getInstalled();

    Collection<ContributedLibraryRelease> releases = editorLibrary.getReleases();
    List<ContributedLibraryRelease> notInstalled = new ArrayList<>(releases);
    if (mayInstalled.isPresent()) {
      notInstalled.remove(mayInstalled.get());
    }

    Collections.sort(notInstalled, new ReverseComparator<>(
        new DownloadableContributionVersionComparator()));

    editorCell.downgradeChooser.removeAllItems();
    editorCell.downgradeChooser.addItem(tr("Select version"));

    final List<ContributedLibraryRelease> notInstalledPrevious = new LinkedList<>();
    final List<ContributedLibraryRelease> notInstalledNewer = new LinkedList<>();

    notInstalled.stream().forEach(input -> {
      if (!mayInstalled.isPresent()
          || VersionComparator.greaterThan(mayInstalled.get(), input)) {
        notInstalledPrevious.add(input);
      } else {
        notInstalledNewer.add(input);
      }
    });
    notInstalledNewer.forEach(editorCell.downgradeChooser::addItem);
    notInstalledPrevious.forEach(editorCell.downgradeChooser::addItem);

    editorCell.downgradeChooser
        .setVisible(mayInstalled.isPresent()
                    && (!notInstalledPrevious.isEmpty()
                        || notInstalledNewer.size() > 1));
    editorCell.downgradeButton
        .setVisible(mayInstalled.isPresent()
                    && (!notInstalledPrevious.isEmpty()
                        || notInstalledNewer.size() > 1));

    editorCell.versionToInstallChooser.removeAllItems();
    notInstalled.forEach(editorCell.versionToInstallChooser::addItem);
    editorCell.versionToInstallChooser
        .setVisible(!mayInstalled.isPresent() && notInstalled.size() > 1);

    editorCell.setForeground(Color.BLACK);
    editorCell.setBackground(new Color(218, 227, 227)); // #dae3e3
    return editorCell;
  }

  @Override
  public void setEnabled(boolean enabled) {
    editorCell.setButtonsVisible(enabled);
  }

  public void setStatus(String status) {
    editorCell.statusLabel.setText(status);
  }

  protected void onRemove(ContributedLibraryRelease selected) {
    // Empty
  }

  protected void onInstall(ContributedLibraryRelease selected,
                           Optional<ContributedLibraryRelease> mayInstalled) {
    // Empty
  }

}
