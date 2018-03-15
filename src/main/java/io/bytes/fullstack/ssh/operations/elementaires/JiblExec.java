package io.bytes.fullstack.ssh.operations.elementaires;

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import com.jcraft.jsch.*;
import java.awt.*;
import javax.swing.*;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;

public class JiblExec {
	
	/**
	 * effectue la commande que je souhaite exécuter en secure shell 
	 * @param commandeAexecuter
	 * @param AcetteAdresseIP
	 * @param AvecCeUser
	 * @param AvecCeMotdepasse
	 * @return le code de retour
	 */
	public static int executeCetteCommande(String commandeAexecuter, String AcetteAdresseIP, String AvecCeUser, String AvecCeMotdepasse) {
		// String[] tableauArgs = {"user@remotehost:nomfinaldufichierCopie"}; // "Enter username@hostname"
		String[] tableauArgs = {AvecCeUser + "@"+AcetteAdresseIP};
		int codeSortieExec = 1;
		
		codeSortieExec = JiblExec.Executer(tableauArgs, AvecCeMotdepasse, commandeAexecuter);

		return codeSortieExec;
	}
	/**
	 * Exécute la co
	 * @param arg
	 * @param motdepasse
	 * @param commandeAexecuter
	 * @return le code de retour de du process ayant exécuté la commande SSH. S'il n'est pas zéro, un problème est survenu pendant l'exécution de la commande.
	 */
	private static int Executer(String[] arg, String motdepasse, String commandeAexecuter) {
		int codeSortieExec = 1; // par défaut, il y a eut un problème
		/**
		 * le canal de communication SSH
		 */
		Channel channel = null;
		/**
		 * la sortie faite par le système après exécution de la commande
		 */
		OutputStream sortieApresExectionDeLaCommande = null;
		try {
			JSch jsch = new JSch();

			String host = null;
			if (arg.length > 0) {
				host = arg[0];
			} else {
				host = JOptionPane.showInputDialog("Enter username@hostname",
						System.getProperty("user.name") + "@localhost");
			}
			String user = host.substring(0, host.indexOf('@'));
			host = host.substring(host.indexOf('@') + 1);

			Session session = jsch.getSession(user, host, 22);

			/*
			 * String xhost="127.0.0.1"; int xport=0; String
			 * display=JOptionPane.showInputDialog("Enter display name", xhost+":"+xport);
			 * xhost=display.substring(0, display.indexOf(':'));
			 * xport=Integer.parseInt(display.substring(display.indexOf(':')+1));
			 * session.setX11Host(xhost); session.setX11Port(xport+6000);
			 */

			// username and password will be given via UserInfo interface.
			UserInfo ui = new JiblUserInfo(motdepasse);
			session.setUserInfo(ui);
			session.connect();

//			String command = JOptionPane.showInputDialog("Enter command", "set|grep SSH");
//			String commandeAexecuter = JOptionPane.showInputDialog("Enter command", "set|grep SSH");
			
			// donc le canal de communication SSH
			channel = session.openChannel("exec");
			sortieApresExectionDeLaCommande = channel.getOutputStream();
			
			((ChannelExec) channel).setCommand(commandeAexecuter);

			// X Forwarding
			// channel.setXForwarding(true);

			 channel.setInputStream(System.in); // pour que je puisse répondre aux demandes envoyées interactivement par les scripts?
//			channel.setInputStream(null);

			// channel.setOutputStream(System.out);

			// FileOutputStream fos=new FileOutputStream("/tmp/stderr");
			// ((ChannelExec)channel).setErrStream(fos);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();

			channel.connect();
			System.out.println(" [+fulltstack] - [" + commandeAexecuter + "]"); // commandeAexecuter
			channel.run();
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					System.out.println(" [+fulltstack] - [exit-status=" + codeSortieExec + "]"); // commandeAexecuter
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			// Affichage de la sortie de la sortie de la commande:
//			lireSortieApresExecutionCommande(channel);
		}
		return 0;
	}
	/**
	 * Pour lire une sortie de commande
	 * @param unCanalSSH
	 */
	private static void lireSortieApresExecutionCommande(Channel unCanalSSH){
	
//		System.out.println("[DEBUT-sortie après exécution-]");
	    byte[] buffer = new byte[1024];
	
	    try {
	        InputStream in = unCanalSSH.getInputStream();
	        String line = "";
	        while (true) {
	            while (in.available() > 0) {
	                int i = in.read(buffer, 0, 1024);
	                if (i < 0) {
	                    break;
	                }
	                line = new String(buffer, 0, i);
	                System.out.println(line);
	            }
	
	            if(line.contains("logout")) {
	                break;
	            }
	
	            if (unCanalSSH.isClosed()) {
	                break;
	            }
	            
	            try {
	                Thread.sleep(1000);
	            } catch (Exception ee){}
	        }
	    } catch (Exception e) {
	        System.out.println("Une exception s'est produite lors de la lecture de la sortie système : "+ e.getMessage());
	        e.printStackTrace();
	    }
	    
//	    System.out.println("[FIN-sortie après exécution-]");
	
	}
	/**
	 * Tous les prompts doivent retrouner "true" pour que l'exécution se fasse sans interactivité à l'utilisateur.
	 * Le mot de passe doit être fixé dans 
	 * @author ezy
	 *
	 */
	public static class JiblUserInfo implements UserInfo/*, UIKeyboardInteractive */{
		public JiblUserInfo(String motdepasse) {
			super();
			this.passwd = motdepasse;
		}
		
		public String getPassword() {
			return passwd;
		}

		public boolean promptYesNo(String str) {
			return true;
		}

		String passwd;
		JTextField passwordField = (JTextField) new JPasswordField(20);

		public String getPassphrase() {
			return null;
		}

		public boolean promptPassphrase(String message) {
			return true;
		}

		public boolean promptPassword(String message) {
			return true;
		}

		public void showMessage(String message) {
		}




	}
}