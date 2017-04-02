import socket
import sys
import cv2
import numpy as np


def main():
    host = sys.argv[1]
    port = 5000

    mySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    mySocket.bind((host, port))

    mySocket.listen(1)
    conn, addr = mySocket.accept()
    print("Connection from: " + str(addr))
    i = 0
    while True:
        i += 1
        buff = b''
        while True:
            c = conn.recv(1)
            if c == b'[':
                continue
            elif c == b']':
                break
            else:
                buff += c

        decoded_buff = buff.decode()
        header = decoded_buff.split(';')
        speed_cmd = int(header[0])
        steering_cmd = int(header[1])
        speed = float(header[2])
        steering = float(header[3])
        size = int(header[4])

        try:
            print(i, speed_cmd, steering_cmd, speed, steering, size)
        except KeyboardInterrupt:
            conn.close()
        in_bytes = b''
        with open('/tmp/image' + str(i) + '.jpeg', 'wb') as f:
            while size > 0:
                data = conn.recv(size)
                in_bytes += data
                f.write(data)
                size -= len(data)

        np_arr = np.fromstring(in_bytes, np.uint8)
        frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
        cv2.imshow('video', frame)
        height, width, channels = frame.shape
        four_cc = cv2.VideoWriter_fourcc(*'XVID')
        out = cv2.VideoWriter('out_video.avi', four_cc, 20, (width, height))

        out.write(frame)
        if (cv2.waitKey(1) & 0xFF) == ord('q'):  # Hit `q` to exit
            break
    out.release()
    cv2.destroyAllWindows()
    sys.exit(0)

if __name__ == '__main__':
    main()