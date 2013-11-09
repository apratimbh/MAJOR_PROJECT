package mp;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import javax.imageio.ImageIO;

public class decision_tree {

	public bundle load(String filename)
	{
		BufferedImage img_org=null,img_seg=null;
		try {
			img_org=ImageIO.read(new File("/home/vivek/im/i/"+filename+".jpg"));
			img_seg=ImageIO.read(new File("/home/vivek/im/a/"+filename+".png"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		int[][] map=new int[img_org.getHeight()][img_org.getWidth()];
		HashMap<Integer,Integer> color_class=new HashMap<Integer,Integer>();
		int color_num=0;
		for(int i=0;i<img_org.getHeight();i++)
		{
			for(int j=0;j<img_org.getWidth();j++)
			{
				int color=img_seg.getRGB(j, i);
				color&=0x0ffffff;
				if(color_class.containsKey(color))
				{
					map[i][j]=color_class.get(color);
				}
				else
				{
					color_num++;
					map[i][j]=color_num;
					color_class.put(color, color_num);
				}
			}
		}
		int[] stat=compute_statistics(map, color_num);
		int smallest_class=1,num_pixels_in_smallest_class=stat[1];
		for(int i=1;i<=stat[0];i++)
		{
			if(stat[i]<num_pixels_in_smallest_class)
			{
				num_pixels_in_smallest_class=stat[i];
				smallest_class=i;
			}
		}
		int needed_pixels=num_pixels_in_smallest_class/8;
		int[] fraction_needed=new int[stat[0]+1];
		for(int i=1;i<=stat[0];i++)
		{
			fraction_needed[i]=stat[i]/needed_pixels;
			
		}
		
		boolean[][] needed_map=new boolean[img_org.getHeight()][img_org.getWidth()];
		Random generator = new Random( 19580427 );
		
		for(int i=0;i<map.length;i++)
		{
			for(int j=0;j<map[0].length;j++)
			{
				int r = generator.nextInt();
				int rem=r%(fraction_needed[map[i][j]]);
				if(rem==0)
				{
					needed_map[i][j]=true;
				}
			}
		}
		return (new bundle(img_org,map,needed_map));
	}
	public int[] compute_statistics(int[][] map,int class_num)
	{
		int[] stat=new int[class_num+1];
		System.out.println("Class_num: "+class_num);
		stat[0]=class_num; // number of color classes
		for(int i=0;i<map.length;i++)
		{
			for(int j=0;j<map[0].length;j++)
			{
				stat[map[i][j]]++;
			}
		}
		return stat;
	}
	public int avg_patch(BufferedImage img,int x,int y,int size)
	{
		int avg=0;
		int r=0,g=0,b=0;
		if(size==4)
		{
			r=(img.getRGB(x,y))>>16&0x000000FF + (img.getRGB(x-1,y))>>16&0x000000FF+(img.getRGB(x,y-1))>>16&0x000000FF+(img.getRGB(x-1,y-1))>>16&0x000000FF;
		g=(img.getRGB(x,y))>>8&0x000000FF + (img.getRGB(x-1,y))>>8&0x000000FF+(img.getRGB(x,y-1))>>8&0x000000FF+(img.getRGB(x-1,y-1))>>8&0x000000FF;
		b=(img.getRGB(x,y))>>0&0x000000FF + (img.getRGB(x-1,y))>>0&0x000000FF+(img.getRGB(x,y-1))>>0&0x000000FF+(img.getRGB(x-1,y-1))>>0&0x000000FF;
		r/=4;
		g/=4;
		b/=4;
		avg=r<<16+g<<8+b;

		}
		else if(size==9)
		{
			int xx=0,yy=0;
			try {
				for(int i1=x-1;i1<=x+1&&i1<img.getHeight();i1++)
				{
					for(int j1=y-1;j1<=y+1&&j1<img.getWidth();j1++)
					{
						xx=i1;yy=j1;
						r+=img.getRGB(j1,i1)>>16&0x000000FF;
					g+=img.getRGB(j1,i1)>>8&0x000000FF;
				b+=img.getRGB(j1,i1)>>0&0x000000FF;
					}
				}
			}
			catch(Exception e)
			{
				System.out.println(xx+" "+yy);
			}
			r/=9;
			g/=9;
			b/=9;
			avg=r<<16+g<<8+b;

		}
		return avg;
	}
	public int[][] superpixel(BufferedImage img,int x,int y)
	{
		int[][] tmp=new int[3][9];
		int c=0,v;
		for(int i1=x-1;i1<=x+1&&i1<img.getHeight();i1++)
		{
			for(int j1=y-1;j1<=y+1&&j1<img.getWidth();j1++)
			{
				v=avg_patch(img,i1,j1,9);
				tmp[0][c]=v>>16&0x000000FF;
			tmp[1][c]=v>>8&0x000000FF;
		tmp[2][c++]=v>>0&0x000000FF;

			}
		}
		return tmp;
	}
	public bundle preprocess(bundle bimg)
	{
		BufferedImage img=bimg.img;
		int map[][]=bimg.map;
		boolean[][] needed_map=bimg.needed_map;
		float[] blurKernel = {
				/*-0, -1, -2, -1, -0,
				-1, -4, -8, -4, -1,
				-2, -8, +64, -8, -2,
				-1, -4, -8, -4, -1,
				-0, -1, -2, -1, -0*/
				1 / 9f, 1 / 9f, 1 / 9f,
				1 / 9f, 1 / 9f, 1 / 9f,
				1 / 9f, 1 / 9f, 1 / 9f
		};
		BufferedImageOp blur = new ConvolveOp(new Kernel(3, 3, blurKernel));
		img = blur.filter(img, new BufferedImage(img.getWidth(),img.getHeight(), img.getType()));
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage down_sampled = new BufferedImage((int)(0.5 * img.getWidth(null)),(int)(0.5 * img.getHeight(null)), BufferedImage.TYPE_INT_ARGB);

		Graphics2D grph = (Graphics2D) down_sampled.getGraphics();
		grph.scale(0.5, 0.5);

		grph.drawImage(img, 0, 0, null);
		grph.dispose();
		int[][] new_map=new int[down_sampled.getHeight()][down_sampled.getWidth()];
		boolean[][] new_needed_map=new boolean[down_sampled.getHeight()][down_sampled.getWidth()];
		int new_x=0,new_y=0,ix=0,iy=0;
		try {
			for(int i=1;i<img.getHeight();i+=2)
			{
				for(int j=1;j<img.getWidth();j+=2)
				{
					ix=i;iy=j;
					new_map[new_x][new_y]=map[i][j];
					if(map[i][j]==0)
					{
						new_map[new_x][new_y]=1; 
					}
					if(needed_map[i-1][j-1]==true||needed_map[i-1][j]==true||needed_map[i][j]==true||needed_map[i][j-1]==true)
					{
						new_needed_map[new_x][new_y]=true;
					}
					new_y++;
				}
				new_y=0;
				new_x++;
			}
			File file1 = new File("new3.png");
			try {
				ImageIO.write(down_sampled, "png", file1);
			}
			catch(Exception e)
			{

			}
		}
		catch(Exception e)
		{
			System.out.println("Size: "+img.getWidth()/2+"x"+img.getHeight()/2+" coordinates: "+new_x+" "+new_y);
			System.out.println("i - j : "+ix+" "+iy);
			e.printStackTrace();
		}
		return (new bundle(down_sampled,new_map,new_needed_map));
	}

	public void create_arff_data(bundle img_bundle,String fname,int iname)
	{
		System.out.println("Img size: "+img_bundle.img.getWidth()+"x"+img_bundle.img.getHeight()+" Map: "+img_bundle.map[0].length+"x"+img_bundle.map.length);
		int[][] fv=new int[3][9];
		int r[] = new int[16];
		int g[] = new int[16];
		int b[] = new int[16];
		try {

			File file = new File(fname);

			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);
			if(iname == 1)
			{
				FileOutputStream writer = new FileOutputStream(file);
				writer.write((new String()).getBytes());
				writer.close();
				bw.write("@RELATION 'decision_tree'\n\n");
				for(int i=0;i<16;i++)
				{
					bw.write("@ATTRIBUTE r"+(i+1)+" NUMERIC\n");
				}
				for(int i=0;i<16;i++)
				{
					bw.write("@ATTRIBUTE g"+(i+1)+" NUMERIC\n");
				}
				for(int i=0;i<16;i++)
				{
					bw.write("@ATTRIBUTE b"+(i+1)+" NUMERIC\n");
				}
				bw.write("@ATTRIBUTE area {1,2,3,4,5}\n");
				bw.write("\n\n@DATA\n\n");
			}
			for(int i=3;i<img_bundle.img.getHeight()-2;i++)
			{
				for(int j=3;j<img_bundle.img.getWidth()-2;j++)
				{
					if(img_bundle.needed_map[i][j] == true)
					{
						for(int k=0;k<16;k++)
						{
							r[k]=0;
							g[k]=0;
							b[k]=0;
						}
						fv=superpixel(img_bundle.img,i,j);


						for(int k=0;k<9;k++)
						{
							r[fv[0][k]/16]++;
							g[fv[1][k]/16]++;
							b[fv[2][k]/16]++;
						}
						for(int k=0;k<16;k++)
							bw.write(String.valueOf(r[k])+",");
						for(int k=0;k<16;k++)
							bw.write(String.valueOf(g[k])+",");
						for(int k=0;k<16;k++)
							bw.write(String.valueOf(b[k])+",");
						bw.write(String.valueOf(img_bundle.map[i][j])+"\n");
					}
				}
			}
			bw.close();

			System.out.println("FILE Done");

		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	public static void main(String[] args)
	{
		decision_tree o=new decision_tree();
		int iname = 1;
		for(iname =1;iname<=50;iname++)
		{
			bundle b1=o.load(String.valueOf(iname));
			bundle b2=o.preprocess(b1);
			File file1 = new File("new1.png");
			File file2 = new File("new2.png");
			try {
				ImageIO.write(b1.img, "png", file1);
				ImageIO.write(b2.img, "png", file2); 
			}
			catch(Exception e)
			{

			}
			o.create_arff_data(b2, "dtree.arff",iname);
			System.out.println("File "+iname+" done");
		}
	}
}
class bundle {
	BufferedImage img=null;
	int[][] map=null;
	boolean[][] needed_map=null;
	public bundle(BufferedImage img,int[][] map,boolean[][] needed_map) {
		this.img=img;
		this.map=map;
		this.needed_map=needed_map;
	}
}
