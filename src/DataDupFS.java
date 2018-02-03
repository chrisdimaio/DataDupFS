import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.lang.Runtime;
import java.lang.Process;

public class DataDupFS
{
	public static void main(String args[])
	{
		String test = "When in the Course of human events, it becomes necessary for one people to dissolve the political bands which have connected them with another, and to assume among the powers of the earth, the separate and equal station to which the Laws of Nature and of Nature's God entitle them, a decent respect to the opinions of mankind requires that they should declare the causes which impel them to the separation. We hold these truths to be self-evident, that all men are created equal, that they are endowed by their Creator with certain unalienable Rights, that among these are Life, Liberty and the pursuit of Happiness.--That to secure these rights, Governments are instituted among Men, deriving their just powers from the consent of the governed, --That whenever any Form of Government becomes destructive of these ends, it is the Right of the People to alter or to abolish it, and to institute new Government, laying its foundation on such principles and organizing its powers in such form, as to them shall seem most likely to effect their Safety and Happiness. Prudence, indeed, will dictate that Governments long established should not be changed for light and transient causes; and accordingly all experience hath shewn, that mankind are more disposed to suffer, while evils are sufferable, than to right themselves by abolishing the forms to which they are accustomed. But when a long train of abuses and usurpations, pursuing invariably the same Object evinces a design to reduce them under absolute Despotism, it is their right, it is their duty, to throw off such Government, and to provide new Guards for their future security.--Such has been the patient sufferance of these Colonies; and such is now the necessity which constrains them to alter their former Systems of Government. The history of the present King of Great Britain is a history of repeated injuries and usurpations, all having in direct object the establishment of an absolute Tyranny over these States. To prove this, let Facts be submitted to a candid world.";
		String test2 = test + test + test + test + test + test + test + test + test + test + test + test + test + test + test + test + test;
		String test3 = test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2 + test2;
		
		BlockArray disk = new BlockArray();
		
		byte[] input = test3.getBytes();
		System.out.println("Input hash: " + simpleHash(input));
		INode DOIINode = createINode(input, disk);
		byte[] output = readINode(DOIINode, disk);
		System.out.println("Ouput hash: " + simpleHash(output));
		
    }
	
	private static byte[] readINode(INode iNode, BlockArray disk)
	{
		int blockSize 	= disk.getBlockSize();
		int dataLength 	= iNode.getLength();
		
		byte[] data = new byte[dataLength];
		
		int numberOfBlocks = dataLength/blockSize;
		if(dataLength % blockSize != 0)
		{
			numberOfBlocks++;
		}
		
		int bytesRemaining = dataLength;
		int stopAt = blockSize;
		for(int i = 0; i < numberOfBlocks; i++)
		{
			if(bytesRemaining < blockSize)
			{
				stopAt = bytesRemaining;
			}
			byte[] tmp = disk.getBlock(iNode.getBlock(), stopAt);
			for(int j = 0; j < stopAt; j++)
			{
				data[j + i * blockSize] = tmp[j];
			}
			bytesRemaining -= stopAt;
		}
		
		return data;
	}
	
	private static INode createINode(byte[] data, BlockArray disk)
	{
		int blockSize 	= disk.getBlockSize();
		int dataLength 	= data.length;
		
		INode iNode = new INode(dataLength);
		// System.out.println(iNode.toString());
		
		int numberOfBlocks = dataLength/blockSize;
		if(dataLength % blockSize != 0)
		{
			numberOfBlocks++;
		}
		
		int bytesRemaining = dataLength;
		int stopAt = blockSize;
		for(int i = 0; i < numberOfBlocks; i++)
		{
			if(bytesRemaining < blockSize)
			{
				stopAt = bytesRemaining;
			}
			byte[] tmp = new byte[stopAt];
			for(int j = 0; j < stopAt; j++)
			{
				tmp[j] = data[j + i * blockSize];
			}
			bytesRemaining -= stopAt;
			iNode.addBlock(disk.writeBytes(tmp));
			// System.out.println(iNode.toString());
		}
		
		return iNode;
	}
	
	private static int simpleHash(byte[] d)
	{
		int hash = 0;
		for(int i = 0; i < d.length; i++)
		{
			hash += d[i];
		}
		return hash;
	}
	
	private static class INode
	{
		static final int NUMBER_OF_BLOCK_POINTERS = 128;
		
		int length 			= 0;
		int blockCounter 	= 0;
		int blockPosition 	= 0;
		
		int[] blockPointers;
		
		INode(int fileLength)
		{
			length = fileLength;
			blockPointers = new int[NUMBER_OF_BLOCK_POINTERS];
		}
		
		public int getLength()
		{
			return length;
		}
		
		public int getBlock()
		{
			
			if(blockPosition == blockCounter)
			{
				blockPosition = 0;
			}
			return blockPointers[blockPosition++];
			
		}
		
		public void addBlock(int blockIndex)
		{
			blockPointers[blockCounter++] = blockIndex;
		}
		
		public void reset()
		{
			length = 0;
			blockCounter = 0;
		}
		
		public String toString()
		{
			String pointersString = "";
			for(int i = 0; i < NUMBER_OF_BLOCK_POINTERS; i++)
			{
				if(i == NUMBER_OF_BLOCK_POINTERS - 1)
				{
					pointersString += blockPointers[i];
				}
				else
				{
					pointersString += blockPointers[i] + ",";
				}
			}
			return "{length=" + length + ", pointers=[" + pointersString + "]}";
		}
	}
	
	private static class BlockArray
	{
		private static final int BLOCK_SIZE_IN_BYTES = 8192;
		private static final int NUMBER_OF_BLOCKS	 = 1024;
		
		private byte[] rawData;
		private boolean[] blockMap;
		
		BlockArray()
		{
			rawData = new byte[NUMBER_OF_BLOCKS * BLOCK_SIZE_IN_BYTES];
			blockMap = new boolean[NUMBER_OF_BLOCKS];
		}
		
		private int getBlockSize(){return BLOCK_SIZE_IN_BYTES;}
		
		public int writeBytes(byte[] data)
		{
			int blockLength = data.length;
			int stopAt = blockLength;
			
			if(blockLength > BLOCK_SIZE_IN_BYTES)
			{
				stopAt = BLOCK_SIZE_IN_BYTES;
			}
		
			int blockIndex = findFreeBlock();
			if(blockIndex != -1)
			{
				int blockPosition = blockIndex * BLOCK_SIZE_IN_BYTES;
				for(int i = 0; i < stopAt; i++)
				{
					rawData[i + blockPosition] = data[i];
				}
				blockMap[blockIndex] = true;
			}
			return blockIndex;
		}
		
		public byte[] getBlock(int index, int length)
		{
			int stopAt = length;
			
			if(length > BLOCK_SIZE_IN_BYTES)
			{
				stopAt = BLOCK_SIZE_IN_BYTES;
			}
			
			byte[] block = new byte[stopAt];
			
			int blockPosition = index * BLOCK_SIZE_IN_BYTES;
			for(int i = 0; i < stopAt; i++)
			{
				block[i] = rawData[i + blockPosition];
			}
		
			return block;
		}
		
		public void freeBlock(int blockIndex)
		{
			blockMap[blockIndex] = false;
		}
		
		private int findFreeBlock()
		{
			int i = -1;
			while(blockMap[++i] != false){}
			return i;
		}
	}
}