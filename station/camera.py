import socket
import sys

def main():
    host = sys.argv[1]
    port = 5000

    mySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    mySocket.bind((host, port))

    mySocket.listen(1)
    conn, addr = mySocket.accept()
    print ("Connection from: " + str(addr))
    i = 0;
    while True:
        i += 1
        buff = ''
        while True:
            c = conn.recv(1)
            if c == '[':
                continue
            elif c == ']':
                break
            else:
                buff += c

        header = buff.split(';')
        speed_cmd = int(header[0])
        steering_cmd = int(header[1])
        speed = float(header[2])
        steering = float(header[3])
        size = int(header[4])


        print(i, speed_cmd, steering_cmd, speed, steering, size)
        with open('/tmp/image' + str(i) + '.jpeg', 'wb') as f:
            while size > 0:
                data = conn.recv(size)
                f.write(data)
                size -= len(data)
    conn.close()

if __name__ == '__main__':
    main()
