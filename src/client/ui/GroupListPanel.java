package client.ui;

import common.Group;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 群聊列表面板（左侧「群聊」标签页内容）。
 */
public class GroupListPanel extends JPanel {

    public interface SelectionListener {
        void onGroupSelected(String groupId, String groupName);
    }

    public interface CreateGroupListener {
        void onCreateGroup(String groupName);
    }

    private static final Color BG = new Color(0xF2F2F2);

    private final DefaultListModel<String> displayModel = new DefaultListModel<String>();
    private final List<String> groupIds = new ArrayList<String>();
    private final List<String> groupNames = new ArrayList<String>();
    private final JList<String> groupList = new JList<String>(displayModel);
    private final JTextField nameField = new JTextField(8);
    private final JButton createButton = new JButton("创建");

    private SelectionListener selectionListener;
    private CreateGroupListener createGroupListener;

    public GroupListPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 6));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                notifySelection();
            }
        });
        add(new JScrollPane(groupList), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBackground(BG);
        bottom.add(new JLabel("群名："), BorderLayout.WEST);
        bottom.add(nameField, BorderLayout.CENTER);
        bottom.add(createButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGroupFromInput();
            }
        });
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createGroupFromInput();
            }
        });
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setCreateGroupListener(CreateGroupListener listener) {
        this.createGroupListener = listener;
    }

    /** 用服务端返回的群列表替换当前列表。 */
    public void setGroups(List<Group> groups) {
        String selectedId = getSelectedGroupId();
        displayModel.clear();
        groupIds.clear();
        groupNames.clear();
        if (groups != null) {
            for (Group group : groups) {
                if (group == null || group.getGroupId() == null || group.getGroupId().trim().isEmpty()) {
                    continue;
                }
                String id = group.getGroupId().trim();
                String name = group.getGroupName();
                if (name == null || name.trim().isEmpty()) {
                    name = id;
                }
                groupIds.add(id);
                groupNames.add(name);
                displayModel.addElement(name + "（ID：" + id + "）");
            }
        }
        if (selectedId != null) {
            int index = groupIds.indexOf(selectedId);
            if (index >= 0) {
                groupList.setSelectedIndex(index);
            }
        }
    }

    public String getSelectedGroupId() {
        int index = groupList.getSelectedIndex();
        if (index < 0 || index >= groupIds.size()) {
            return null;
        }
        return groupIds.get(index);
    }

    public String getSelectedGroupName() {
        int index = groupList.getSelectedIndex();
        if (index < 0 || index >= groupNames.size()) {
            return null;
        }
        return groupNames.get(index);
    }

    private void createGroupFromInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = "未命名群聊";
        }
        if (createGroupListener != null) {
            createGroupListener.onCreateGroup(name);
        }
        nameField.setText("");
    }

    private void notifySelection() {
        if (selectionListener == null) {
            return;
        }
        String groupId = getSelectedGroupId();
        if (groupId != null) {
            selectionListener.onGroupSelected(groupId, getSelectedGroupName());
        }
    }
}
