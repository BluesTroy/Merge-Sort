import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


public class MergeSort {

	private static final int RECORD_NUMBER =10000000;
	private static final int FILES_NUMBER = 20;
	private static final int SUB_RECORD_NUMBER = 500000; //RECORD_NUMBER/FILES_NUMBER
	private static final int RECORD_SIZE = 100; //96B
	private static final int RANDOM_SCOPE = 1000000;
	
	private static final byte[] content =  new byte[RECORD_SIZE];//[CONTENT_SIZE];  一个记录的内容
	private String filePath = "G:\\File";
	
	
	private static final int BUFFER_NUMBER = 21;  //21个缓冲块，其中20个输入缓冲块，1个输出缓冲块
	private static final int BUFFER_RECORD_NUMBER = 40; //一个缓冲块4096B，可以存放4096/100个记录
														//--->修改第二阶段算法时，缓冲块大小为1M,1M/100=10485, 取为10000
	private byte [][][] buffer = null;  ////用21个二维字节数组模拟21个缓冲块，其中buffer[0~19]是输入缓冲块，buffer[20]是输出缓冲块
	
	private int byte4ToInt(byte[] bytes) {   //将4个byte数转化为4字节的Int
		int b0 = bytes[0] & 0xFF;
		int b1 = bytes[1] & 0xFF;
		int b2 = bytes[2] & 0xFF;
		int b3 = bytes[3] & 0xFF;
		return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
	}

	private byte[] intToByte4(int i) {   ////将4字节的int转化为4个byte
		byte[] targets = new byte[4];
		targets[3] = (byte) (i & 0xFF);
		targets[2] = (byte) (i >> 8 & 0xFF);
		targets[1] = (byte) (i >> 16 & 0xFF);
		targets[0] = (byte) (i >> 24 & 0xFF);
		return targets;
	}

	
	public void generateRecord(){   //生成一条记录
		Random random = new Random();
		int A = Math.abs(random.nextInt())%RANDOM_SCOPE;   //记录的key  A随机生成
		for(int i =0;i <4;i++){               	//前四个字节表示A，后面的96字节默认为0
			content[i]=intToByte4(A)[i];
		}
	}

	public void generateFiles(){		//生成20个子文件
		File file = null;
		FileOutputStream fos = null;
		for(int i=0; i<FILES_NUMBER; i++){
			file = new File(filePath+(i+1));
			if(!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				fos = new FileOutputStream(file);
				for(int k=0;k<SUB_RECORD_NUMBER;k++){	//向每个子文件中写入SUB_RECORD_NUMBER个记录
					generateRecord();
					fos.write(content);
					
				}
				System.out.println("Generate "+filePath+(i+1)+" successfully!");
				file = null;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				try {
					fos.close();
					file = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}
	
	public void firstPhase(){
		byte [][] memory = new byte[SUB_RECORD_NUMBER][RECORD_SIZE];  //模拟50M内存，内存全部存成byte数组
		for(int i = 0 ;i < FILES_NUMBER;i++){   ///分20次将所有记录读进去，每次读50M，即一个子文件
			File file = new File(filePath+(i+1));
			try {
				//将子文件中的记录读到内存中
				FileInputStream fis = new FileInputStream(file);
				for(int k=0;k<SUB_RECORD_NUMBER;k++){
					fis.read(memory[k]);
//					if(k<3){System.out.println(byte4ToInt(memory[k]));}
				
				}//此时，内存中是一个子文件的所有记录，并占满内存
				fis.close();
				quickSort(memory, 0,SUB_RECORD_NUMBER-1);
//					System.out.println("ddddd:");
//				for(int k=0;k<10;k++){
//					System.out.println(byte4ToInt(memory[k]));
//				}
				//将排序后的记录重写到对应的子文件中
				FileOutputStream fos = new FileOutputStream(file);
				for(int k=0;k<SUB_RECORD_NUMBER;k++){
					fos.write(memory[k]);
				}
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Sort "+filePath+(i+1)+ " finished!");
		}
	}
	
	
	public void secondPhase(){
		//用21个二维字节数组模拟21个缓冲块，其中buffer[0~19]是输入缓冲块，buffer[20]是输出缓冲块
		buffer = new byte[BUFFER_NUMBER][BUFFER_RECORD_NUMBER][RECORD_SIZE];
		File file = null;

		
		FileOutputStream fos = null;    //1个文件输出流，向内对应buffer[20]，向外对应最终排好序的总文件File
		FileInputStream fis[] = new FileInputStream[20];   //20个文件输入流fis[0~19]，分别向内对应buffer[0~19],向外对应20个子文件File1~File20，
		for(int i=0; i< FILES_NUMBER; i++){
			file = new File(filePath+(i+1));
			try {
				fis[i] = new FileInputStream(file);
				for(int k=0; k<BUFFER_RECORD_NUMBER;k++){
					fis[i].read(buffer[i][k]); 	//从文件输入流fis[i]中读取（第File[i+1]个文件中的）一个记录到输入缓冲区buffer[i]，一共读取k个，缓冲区就满了
//					System.out.println(byte4ToInt(buffer[i][k]));
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		file = new File(filePath);
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//TODO 取每个缓冲区的第一个记录，选最小的那个放到输出缓冲区中
		int minIndex = 0;	//记录20个缓冲输入块的第一个元素中最小的记录的索引
		int index[] = new int[20]; //用20个整数记录20个缓冲块当前元素的下标，初始值为0
		int outputBufferIndex = 0; //记录输出缓冲区的第一个为空的索引
		int avaiableBufferNum = 20;
		while(true){
			minIndex=0;
			while(buffer[minIndex] == null){	//从第一个不为null的缓冲块开始
				minIndex++;
			}
			if(avaiableBufferNum == 1){ //如果只剩最后一个缓冲块，
				writeToDiskFromOutputBuffer(buffer[20],fos,outputBufferIndex);	//先将输出缓冲块中的数据写到磁盘File文件中
				buffer[20]=null;  //回收输出缓冲区内从空间
				writeToDiskFromTheLastInputBuffer(buffer[minIndex],index[minIndex],fos);  //再把最后一个输入缓冲块的剩余数据(从index[minIndex]处开始)写到磁盘
				break;
			}
			for(int i  = minIndex; i< FILES_NUMBER;i++){
				if(buffer[i]==null){ //如果buffer[i]为null时，说明对应的文件已读完，不考虑此缓冲块   //TODO 文件读到末尾将缓冲块回收null
					avaiableBufferNum--;//剩余输入缓冲块的个数
//					System.out.println("avaiableBufferNum"+avaiableBufferNum);
					continue;
				}
				if(index[i]>=BUFFER_RECORD_NUMBER){	//缓冲块buffer[i]空时，重新从文件读入后面的数据
					readToInputBuffer(i,fis[i]);
					if(buffer[i]==null){ //如果buffer[i]为null时，说明对应的文件已读完，不考虑此缓冲块   //TODO 文件读到末尾将缓冲块回收null
						avaiableBufferNum--;
//						System.out.println("avaiableBufferNum"+avaiableBufferNum);
						continue;
					}
					index[i]=0;
				}
				if(byte4ToInt(buffer[i][index[i]])<byte4ToInt(buffer[minIndex][index[minIndex]])){
					minIndex=i;
				}
			}
			buffer[20][outputBufferIndex++]=buffer[minIndex][index[minIndex]++];
			if(outputBufferIndex>=BUFFER_RECORD_NUMBER){	//输出缓冲区满时，将输出缓冲区中的数据写到磁盘File文件中
				writeToDiskFromOutputBuffer(buffer[20],fos,outputBufferIndex);
				outputBufferIndex=0;
			}
		}
		try {
			fos.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		for(int i = 0 ;i<20;i++){
			try {
				fis[i].close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Generate "+filePath+ " finished!");
	}

	//当输出缓冲块满时将输出缓冲块中的数据写到磁盘上
	private void writeToDiskFromOutputBuffer(byte[][] bs, FileOutputStream fos, int outputBufferIndex) {   
		for(int i=0;i<outputBufferIndex;i++){
			try {
				fos.write(bs[i]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	//第二阶段将要结束时，把最后一个输入缓冲块中的剩余数据写到磁盘中
	private void writeToDiskFromTheLastInputBuffer(byte[][] bs, int offset, FileOutputStream fos) {
		for(int k =offset;k<bs.length;k++){
			try {
				fos.write(bs[k]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	//当某个输入缓冲块中的数据已经被比较完后，从磁盘中重新读新的数据到该输入缓冲块中
	private void readToInputBuffer(int n, FileInputStream fis) {
		for(int i = 0;i<buffer[n].length;i++){
			try {
				if(fis.read(buffer[n][i])== -1 ){//因为一个子文件的byte数正好是缓冲块的倍数，因此应该正好读完，要再读时i=0就返回-1了，不然还要判断读了多少条记录
					buffer[n] = null;    //将这个缓冲块置null回收
					System.out.println("buffer "+ n +"is null!");
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}

	//快速排序，用于给每个子文件排序
	public void quickSort(byte a[][], int left, int right){
		if(left>=right){
			return ;
		}
		int i = left;
		int j = right;
		byte key[] = a[i];
		while(i<j){
			while(i<j && byte4ToInt(a[j])>=byte4ToInt(key)){
				j--;
			}
			if(i<j){
				a[i]=a[j];
				i++;
			}
			while(i<j && byte4ToInt(a[i])<=byte4ToInt(key)){
				i++;
			}
			if(i<j){
				a[j]=a[i];
				j--; 
			}
		}
		a[i]=key;
		quickSort(a, left, i-1);
		quickSort(a,i+1,right);
		
	}
	//从最终排好序的文件中读前10000个记录，输出A值看是否已经排好序
	public void readFiles(){
		File file = null;
		DataInputStream dos= null;
		file = new File(filePath);
		try {
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			for(int i = 0 ;i<1000;i++){
				System.out.println(dis.readInt());
				dis.skipBytes(96);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		MergeSort mergeSort = new MergeSort();
		long startTime ;
		long endTime ;
		
//		startTime = System.currentTimeMillis();
//		mergeSort.generateFiles();
//		endTime = System.currentTimeMillis();
//		System.out.println(">>>>>>>>>Generate all files time: "+(endTime-startTime)+" ms");
//		mergeSort.readFiles();
////		
//		startTime = System.currentTimeMillis();
//		mergeSort.firstPhase();
//		endTime = System.currentTimeMillis();
//		System.out.println(">>>>>>>>>(The first phase)Quick sort sub files time: "+(endTime-startTime)+" ms");
//		mergeSort.readFiles();
		startTime = System.currentTimeMillis();
		mergeSort.secondPhase();
		endTime = System.currentTimeMillis();
		System.out.println(">>>>>>>>>(The second phase)Merge sort time: "+(endTime-startTime)+" ms");
		mergeSort.readFiles();
	}

}
