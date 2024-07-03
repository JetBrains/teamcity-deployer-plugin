package jetbrains.buildServer.deployer.agent.ssh;

import com.jcraft.jsch.USocketFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Set;

public class RFAUSocketFactory implements USocketFactory {
    public static final String PIPE_OPENSSH_SSH_AGENT = "\\\\.\\pipe\\openssh-ssh-agent";

    //@Override
    public SocketChannel connect(String path) throws IOException {
        if (path == null)
            path = PIPE_OPENSSH_SSH_AGENT;
        return new RandomAccessFileSocket(path);
    }

    @Override
    public SocketChannel connect(Path path) throws IOException {
        return new RandomAccessFileSocket(path.toString());
    }

    @Override
    public ServerSocketChannel bind(Path path) throws IOException {
        return null;
    }

    private class RandomAccessFileSocket extends SocketChannel {
        RandomAccessFile raf;

        public RandomAccessFileSocket(String path) throws FileNotFoundException {
            super(null);
            raf = new RandomAccessFile(path, "rw");
        }

        //@Override
        //public int readFull(byte[] buf, int s, int len) throws IOException {
        //    return raf.read(buf, s, len);
        //}
        //
        //@Override
        //public void write(byte[] buf, int s, int len) throws IOException {
        //    raf.write(buf, s, len);
        //}
        //
        //@Override
        //public void close() throws IOException {
        //    raf.close();
        //}

        @Override
        public SocketChannel bind(SocketAddress local) throws IOException {
            return null;
        }

        @Override
        public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
            return null;
        }

        @Override
        public <T> T getOption(SocketOption<T> name) throws IOException {
            return null;
        }

        @Override
        public Set<SocketOption<?>> supportedOptions() {
            return null;
        }

        @Override
        public SocketChannel shutdownInput() throws IOException {
            return null;
        }

        @Override
        public SocketChannel shutdownOutput() throws IOException {
            return null;
        }

        @Override
        public Socket socket() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isConnectionPending() {
            return false;
        }

        @Override
        public boolean connect(SocketAddress remote) throws IOException {
            return false;
        }

        @Override
        public boolean finishConnect() throws IOException {
            return false;
        }

        @Override
        public SocketAddress getRemoteAddress() throws IOException {
            return null;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return raf.read(dst.array(), dst.position(), dst.limit());
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return 0;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            raf.write(src.array(), src.position(), src.limit());
            return src.remaining();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return 0;
        }

        @Override
        public SocketAddress getLocalAddress() throws IOException {
            return null;
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {

        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {

        }
    }
}
