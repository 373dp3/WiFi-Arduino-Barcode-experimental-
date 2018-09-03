namespace WiFiBarcodeConfigSetter
{
    partial class Form1
    {
        /// <summary>
        /// 必要なデザイナー変数です。
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// 使用中のリソースをすべてクリーンアップします。
        /// </summary>
        /// <param name="disposing">マネージ リソースを破棄する場合は true を指定し、その他の場合は false を指定します。</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows フォーム デザイナーで生成されたコード

        /// <summary>
        /// デザイナー サポートに必要なメソッドです。このメソッドの内容を
        /// コード エディターで変更しないでください。
        /// </summary>
        private void InitializeComponent()
        {
            this.comboBoxPort = new System.Windows.Forms.ComboBox();
            this.buttonUpdate = new System.Windows.Forms.Button();
            this.label1 = new System.Windows.Forms.Label();
            this.textBoxServerIp = new System.Windows.Forms.TextBox();
            this.checkBoxDHCP = new System.Windows.Forms.CheckBox();
            this.label2 = new System.Windows.Forms.Label();
            this.textBoxIP = new System.Windows.Forms.TextBox();
            this.textBoxSubnetMask = new System.Windows.Forms.TextBox();
            this.textBoxGateway = new System.Windows.Forms.TextBox();
            this.textBoxDNS = new System.Windows.Forms.TextBox();
            this.label3 = new System.Windows.Forms.Label();
            this.label4 = new System.Windows.Forms.Label();
            this.label5 = new System.Windows.Forms.Label();
            this.label6 = new System.Windows.Forms.Label();
            this.textBoxSSID = new System.Windows.Forms.TextBox();
            this.textBoxPASSWD = new System.Windows.Forms.TextBox();
            this.label7 = new System.Windows.Forms.Label();
            this.textBoxSheetName = new System.Windows.Forms.TextBox();
            this.label8 = new System.Windows.Forms.Label();
            this.buttonWrite = new System.Windows.Forms.Button();
            this.textBoxMessage = new System.Windows.Forms.TextBox();
            this.button1 = new System.Windows.Forms.Button();
            this.SuspendLayout();
            // 
            // comboBoxPort
            // 
            this.comboBoxPort.FormattingEnabled = true;
            this.comboBoxPort.Location = new System.Drawing.Point(12, 11);
            this.comboBoxPort.Name = "comboBoxPort";
            this.comboBoxPort.Size = new System.Drawing.Size(121, 20);
            this.comboBoxPort.TabIndex = 0;
            // 
            // buttonUpdate
            // 
            this.buttonUpdate.Location = new System.Drawing.Point(139, 9);
            this.buttonUpdate.Name = "buttonUpdate";
            this.buttonUpdate.Size = new System.Drawing.Size(156, 23);
            this.buttonUpdate.TabIndex = 1;
            this.buttonUpdate.Text = "ポート一覧更新";
            this.buttonUpdate.UseVisualStyleBackColor = true;
            this.buttonUpdate.Click += new System.EventHandler(this.buttonUpdate_Click);
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(12, 42);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(38, 12);
            this.label1.TabIndex = 2;
            this.label1.Text = "Server";
            // 
            // textBoxServerIp
            // 
            this.textBoxServerIp.Location = new System.Drawing.Point(99, 39);
            this.textBoxServerIp.Name = "textBoxServerIp";
            this.textBoxServerIp.Size = new System.Drawing.Size(196, 19);
            this.textBoxServerIp.TabIndex = 3;
            this.textBoxServerIp.Text = "http://";
            // 
            // checkBoxDHCP
            // 
            this.checkBoxDHCP.AutoSize = true;
            this.checkBoxDHCP.Location = new System.Drawing.Point(99, 117);
            this.checkBoxDHCP.Name = "checkBoxDHCP";
            this.checkBoxDHCP.Size = new System.Drawing.Size(55, 16);
            this.checkBoxDHCP.TabIndex = 6;
            this.checkBoxDHCP.Text = "DHCP";
            this.checkBoxDHCP.UseVisualStyleBackColor = true;
            this.checkBoxDHCP.CheckedChanged += new System.EventHandler(this.checkBoxDHCP_CheckedChanged);
            // 
            // label2
            // 
            this.label2.AutoSize = true;
            this.label2.Location = new System.Drawing.Point(14, 143);
            this.label2.Name = "label2";
            this.label2.Size = new System.Drawing.Size(15, 12);
            this.label2.TabIndex = 6;
            this.label2.Text = "IP";
            // 
            // textBoxIP
            // 
            this.textBoxIP.Location = new System.Drawing.Point(99, 140);
            this.textBoxIP.Name = "textBoxIP";
            this.textBoxIP.Size = new System.Drawing.Size(196, 19);
            this.textBoxIP.TabIndex = 7;
            // 
            // textBoxSubnetMask
            // 
            this.textBoxSubnetMask.Location = new System.Drawing.Point(99, 166);
            this.textBoxSubnetMask.Name = "textBoxSubnetMask";
            this.textBoxSubnetMask.Size = new System.Drawing.Size(196, 19);
            this.textBoxSubnetMask.TabIndex = 8;
            // 
            // textBoxGateway
            // 
            this.textBoxGateway.Location = new System.Drawing.Point(99, 192);
            this.textBoxGateway.Name = "textBoxGateway";
            this.textBoxGateway.Size = new System.Drawing.Size(196, 19);
            this.textBoxGateway.TabIndex = 9;
            // 
            // textBoxDNS
            // 
            this.textBoxDNS.Location = new System.Drawing.Point(99, 218);
            this.textBoxDNS.Name = "textBoxDNS";
            this.textBoxDNS.Size = new System.Drawing.Size(196, 19);
            this.textBoxDNS.TabIndex = 10;
            // 
            // label3
            // 
            this.label3.AutoSize = true;
            this.label3.Location = new System.Drawing.Point(14, 169);
            this.label3.Name = "label3";
            this.label3.Size = new System.Drawing.Size(71, 12);
            this.label3.TabIndex = 11;
            this.label3.Text = "Subnet mask";
            // 
            // label4
            // 
            this.label4.AutoSize = true;
            this.label4.Location = new System.Drawing.Point(14, 195);
            this.label4.Name = "label4";
            this.label4.Size = new System.Drawing.Size(49, 12);
            this.label4.TabIndex = 12;
            this.label4.Text = "Gateway";
            // 
            // label5
            // 
            this.label5.AutoSize = true;
            this.label5.Location = new System.Drawing.Point(14, 224);
            this.label5.Name = "label5";
            this.label5.Size = new System.Drawing.Size(28, 12);
            this.label5.TabIndex = 13;
            this.label5.Text = "DNS";
            // 
            // label6
            // 
            this.label6.AutoSize = true;
            this.label6.Location = new System.Drawing.Point(12, 68);
            this.label6.Name = "label6";
            this.label6.Size = new System.Drawing.Size(30, 12);
            this.label6.TabIndex = 14;
            this.label6.Text = "SSID";
            // 
            // textBoxSSID
            // 
            this.textBoxSSID.Location = new System.Drawing.Point(99, 65);
            this.textBoxSSID.Name = "textBoxSSID";
            this.textBoxSSID.Size = new System.Drawing.Size(196, 19);
            this.textBoxSSID.TabIndex = 4;
            // 
            // textBoxPASSWD
            // 
            this.textBoxPASSWD.Location = new System.Drawing.Point(99, 91);
            this.textBoxPASSWD.Name = "textBoxPASSWD";
            this.textBoxPASSWD.Size = new System.Drawing.Size(196, 19);
            this.textBoxPASSWD.TabIndex = 5;
            // 
            // label7
            // 
            this.label7.AutoSize = true;
            this.label7.Location = new System.Drawing.Point(14, 94);
            this.label7.Name = "label7";
            this.label7.Size = new System.Drawing.Size(67, 12);
            this.label7.TabIndex = 17;
            this.label7.Text = "PASSWORD";
            // 
            // textBoxSheetName
            // 
            this.textBoxSheetName.Location = new System.Drawing.Point(99, 244);
            this.textBoxSheetName.Name = "textBoxSheetName";
            this.textBoxSheetName.Size = new System.Drawing.Size(196, 19);
            this.textBoxSheetName.TabIndex = 11;
            // 
            // label8
            // 
            this.label8.AutoSize = true;
            this.label8.Location = new System.Drawing.Point(12, 247);
            this.label8.Name = "label8";
            this.label8.Size = new System.Drawing.Size(69, 12);
            this.label8.TabIndex = 19;
            this.label8.Text = "初期シート名";
            // 
            // buttonWrite
            // 
            this.buttonWrite.Location = new System.Drawing.Point(99, 269);
            this.buttonWrite.Name = "buttonWrite";
            this.buttonWrite.Size = new System.Drawing.Size(196, 23);
            this.buttonWrite.TabIndex = 12;
            this.buttonWrite.Text = "書き込み";
            this.buttonWrite.UseVisualStyleBackColor = true;
            this.buttonWrite.Click += new System.EventHandler(this.buttonWrite_Click);
            // 
            // textBoxMessage
            // 
            this.textBoxMessage.Location = new System.Drawing.Point(12, 298);
            this.textBoxMessage.Multiline = true;
            this.textBoxMessage.Name = "textBoxMessage";
            this.textBoxMessage.Size = new System.Drawing.Size(283, 73);
            this.textBoxMessage.TabIndex = 21;
            // 
            // button1
            // 
            this.button1.Location = new System.Drawing.Point(194, 116);
            this.button1.Name = "button1";
            this.button1.Size = new System.Drawing.Size(101, 23);
            this.button1.TabIndex = 22;
            this.button1.Text = "ポートを閉じる";
            this.button1.UseVisualStyleBackColor = true;
            this.button1.Click += new System.EventHandler(this.button1_Click);
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 12F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(307, 383);
            this.Controls.Add(this.button1);
            this.Controls.Add(this.textBoxMessage);
            this.Controls.Add(this.buttonWrite);
            this.Controls.Add(this.label8);
            this.Controls.Add(this.textBoxSheetName);
            this.Controls.Add(this.label7);
            this.Controls.Add(this.textBoxPASSWD);
            this.Controls.Add(this.textBoxSSID);
            this.Controls.Add(this.label6);
            this.Controls.Add(this.label5);
            this.Controls.Add(this.label4);
            this.Controls.Add(this.label3);
            this.Controls.Add(this.textBoxDNS);
            this.Controls.Add(this.textBoxGateway);
            this.Controls.Add(this.textBoxSubnetMask);
            this.Controls.Add(this.textBoxIP);
            this.Controls.Add(this.label2);
            this.Controls.Add(this.checkBoxDHCP);
            this.Controls.Add(this.textBoxServerIp);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.buttonUpdate);
            this.Controls.Add(this.comboBoxPort);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedDialog;
            this.Name = "Form1";
            this.Text = "WiFiバーコード設定ツール";
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.Form1_FormClosing);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.ComboBox comboBoxPort;
        private System.Windows.Forms.Button buttonUpdate;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.TextBox textBoxServerIp;
        private System.Windows.Forms.CheckBox checkBoxDHCP;
        private System.Windows.Forms.Label label2;
        private System.Windows.Forms.TextBox textBoxIP;
        private System.Windows.Forms.TextBox textBoxSubnetMask;
        private System.Windows.Forms.TextBox textBoxGateway;
        private System.Windows.Forms.TextBox textBoxDNS;
        private System.Windows.Forms.Label label3;
        private System.Windows.Forms.Label label4;
        private System.Windows.Forms.Label label5;
        private System.Windows.Forms.Label label6;
        private System.Windows.Forms.TextBox textBoxSSID;
        private System.Windows.Forms.TextBox textBoxPASSWD;
        private System.Windows.Forms.Label label7;
        private System.Windows.Forms.TextBox textBoxSheetName;
        private System.Windows.Forms.Label label8;
        private System.Windows.Forms.Button buttonWrite;
        private System.Windows.Forms.TextBox textBoxMessage;
        private System.Windows.Forms.Button button1;
    }
}

