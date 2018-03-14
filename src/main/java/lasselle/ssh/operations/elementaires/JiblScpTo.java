package lasselle.ssh.operations.elementaires;

import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;
import java.io.*;

/**
 * D'après la docuemntation que j'ai trouvé, voci  comment utiliser cette classe en Natif:
 * 
 * java ScpTo file1 user@remotehost:file2
 * 
 * @author ezy
 *
 */
public class JiblScpTo {
	
	/**
	 * effectue la copie que je souhaite faire
	 * @param deCeFichier
	 * @param AcetteAdresseIP
	 * @param AvecCeUser
	 * @param AvecCeMotdepasse
	 */
	public static void faisCopie(String deCeFichier, String nomDuFichierWarSeul, String AcetteAdresseIP, String AvecCeUser, String AvecCeMotdepasse) {
		// String[] tableauArgs = {"nomfichierAcopier", "user@remotehost:nomfinaldufichierCopie"};
		// même nom de fichier  en résultat de copie
		String nomfinaldufichierCopie = nomDuFichierWarSeul;
		String[] tableauArgs = {deCeFichier, AvecCeUser + "@"+AcetteAdresseIP+":" + nomfinaldufichierCopie};
		JiblScpTo.Executer(tableauArgs, AvecCeMotdepasse);
	}
	/**
	 * Anciennement: public static void main(String[] arg)
	 * 
	 * Donc, maintenant:
	 * 
	 * String[] tableauArgs = {"file1", "user@remotehost:file2"};
	 * JiblScpTo.Executer(tableauArgs, "laur1@ne");
	 * // tableauArgs = {"file1", "user@remotehost:file2"} Correspond à:
	 * // 	# ligne de commande shell ou MS DOS
	 * //	  [java ScpTo file1 user@remotehost:file2]
	 * @param arg
	 */
	private static void Executer(String[] arg, String motdepasse) {
		
		
		
		if (arg.length != 2) {
			System.err.println("usage: java ScpTo file1 user@remotehost:file2"); /// coorespond à appeler main avec le tableau // tableauArgs = {"ScpTo", "file1", "user@remotehost:file2"}
			System.exit(-1);
		}

		FileInputStream fis = null;
		try {

			String lfile = arg[0];
			String user = arg[1].substring(0, arg[1].indexOf('@'));
			arg[1] = arg[1].substring(arg[1].indexOf('@') + 1);
			String host = arg[1].substring(0, arg[1].indexOf(':'));
			String rfile = arg[1].substring(arg[1].indexOf(':') + 1);

			JSch jsch = new JSch();
			System.out.println(" +++++++++++++ JIBL VERIF : user >> " + user);
			System.out.println(" +++++++++++++ JIBL VERIF : host >> " + host);
//			System.out.println(" +++++++++++++ JIBL VERIF : CCC" + CCC);
//			System.out.println(" +++++++++++++ JIBL VERIF : CCC" + CCC);
			
//			jsch.setKnownHosts(filename);
//			jsch.se
			Session session = jsch.getSession(user, host, 22);
			// rajout jibl
			session.setPassword(motdepasse);
//			String userconfigAvant = session.getConfig("user");
//			String hostconfigAvant = session.getConfig("host");
//			session.getUserName();
//			session.getHost();
//			session.setHost(host);
//			session.setConfig("StrictHostKeyChecking", "no");
			
//			java.util.Properties configSansCheckHostKey = new java.util.Properties(); 
//			configSansCheckHostKey.put("StrictHostKeyChecking", "no");
//			session.setConfig(configSansCheckHostKey);
			

			// username and password will be given via UserInfo interface.
			UserInfo ui = new JiblUserInfo(motdepasse);
			session.setUserInfo(ui);
			session.connect();

			boolean ptimestamp = true;

			// exec 'scp -t rfile' remotely
			String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			
//			channel.setInputStream(null);
			// get I/O streams for remote scp
			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			if (checkAck(in) != 0) {
				System.exit(0);
			}

			File _lfile = new File(lfile);

			if (ptimestamp) {
				command = "T " + (_lfile.lastModified() / 1000) + " 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
				out.write(command.getBytes());
				out.flush();
				if (checkAck(in) != 0) {
					System.exit(0);
				}
			}

			// send "C0644 filesize filename", where filename should not include '/'
			long filesize = _lfile.length();
			command = "C0644 " + filesize + " ";
			if (lfile.lastIndexOf('/') > 0) {
				command += lfile.substring(lfile.lastIndexOf('/') + 1);
			} else {
				command += lfile;
			}
			command += "\n";
			out.write(command.getBytes());
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}

			// send a content of lfile
			fis = new FileInputStream(lfile);
			byte[] buf = new byte[1024];
			while (true) {
				int len = fis.read(buf, 0, buf.length);
				if (len <= 0)
					break;
				out.write(buf, 0, len); // out.flush();
			}
			fis.close();
			fis = null;
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			if (checkAck(in) != 0) {
				System.exit(0);
			}
			out.close();

			channel.disconnect();
			session.disconnect();

			System.exit(0);
		} catch (Exception e) {
			System.out.println(e);
			try {
				if (fis != null)
					fis.close();
			} catch (Exception ee) {
			}
		}
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

	/**
	 * Tous les prompts doivent retrouner "true" pour que l'exécution se fasse sans interactivité à l'utilisateur.
	 * Le mot de passe doit être fixé dans 
	 * @author ezy
	 *
	 */
	public static class JiblUserInfo implements UserInfo/*, UIKeyboardInteractive */{
//		public JiblUserInfo() {
//			super();
//		}
		public JiblUserInfo(String motdepasse) {
			super();
			this.passwd = motdepasse;
		}
		
		public String getPassword() {
			return passwd;
		}

		public boolean promptYesNo(String str) {
			return false;
		}

		String passwd;

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return false;
		}

		public boolean promptPassword(String message) {
			return false;
		}

		public void showMessage(String message) {
		}




	}
}