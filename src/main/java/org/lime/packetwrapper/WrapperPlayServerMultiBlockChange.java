/**
 * PacketWrapper - ProtocolLib wrappers for Minecraft packets
 * Copyright (C) dmulloy2 <http://dmulloy2.net>
 * Copyright (C) Kristian S. Strangeland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.lime.packetwrapper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.protocol.game.PacketPlayOutMultiBlockChange;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class WrapperPlayServerMultiBlockChange extends AbstractPacket {
	public static final PacketType TYPE =
			PacketType.Play.Server.MULTI_BLOCK_CHANGE;

	public static class SectionPosition {
		public int x;
		public int y;
		public int z;

		private SectionPosition(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public SectionPosition(Vector location) {
			this(location.getBlockX() >> 4, location.getBlockY() >> 4, location.getBlockZ() >> 4);
		}
		public SectionPosition(long packed) {
			this((int)(packed >> 42), (int)(packed << 44 >> 44), (int)(packed << 22 >> 42));
		}
		private static SectionPosition ofSection(BlockPosition pos) {
			return new SectionPosition(pos.getX(), pos.getY(), pos.getZ());
		}

		public long packed() {
			return ((long)x & 0x3FFFFFL) << 42 | (long)y & 0xFFFFFL | ((long)z & 0x3FFFFFL) << 20;
		}

		public int getPackedX(short packedLocalPos) { return (x << 4) + (packedLocalPos >>> 8 & 15); }
		public int getPackedY(short packedLocalPos) { return (y << 4) + (packedLocalPos >>> 0 & 15); }
		public int getPackedZ(short packedLocalPos) { return (z << 4) + (packedLocalPos >>> 4 & 15); }
		public Vector getPacked(short packedLocalPos) { return new Vector(getPackedX(packedLocalPos), getPackedY(packedLocalPos), getPackedZ(packedLocalPos)); }
	}

	public WrapperPlayServerMultiBlockChange() {
		super(new PacketContainer(TYPE), TYPE);
		handle.getModifier().writeDefaults();
	}

	public WrapperPlayServerMultiBlockChange(PacketContainer packet) {
		super(packet, TYPE);
	}

	public SectionPosition getSection() {
		return SectionPosition.ofSection(handle.getSectionPositions().read(0));
	}
	public void setSection(SectionPosition section) {
		handle.getSectionPositions().write(0, new BlockPosition(section.x, section.y, section.z));
	}

	public boolean getIsLightingUpdates() {
		return !handle.getBooleans().read(0);
	}
	public void setIsLightingUpdates(boolean value) {
		handle.getBooleans().write(0, !value);
	}

	public void setChangeData(Map<Short, WrappedBlockData> blocks) {
		WrappedBlockData[] blockData = blocks.values().toArray(WrappedBlockData[]::new);
		short[] blockLocations = ArrayUtils.toPrimitive(blocks.keySet().toArray(Short[]::new));

		handle.getBlockDataArrays().writeSafely(0, blockData);
		handle.getShortArrays().writeSafely(0, blockLocations);
	}
	public Map<Short, WrappedBlockData> getChangeData() {
		WrappedBlockData[] blockData = handle.getBlockDataArrays().readSafely(0);
		short[] blockLocations = handle.getShortArrays().readSafely(0);
		int length = blockData.length;
		HashMap<Short, WrappedBlockData> map = new HashMap<>();
		for (int i = 0; i < length; i++) map.put(blockLocations[i], blockData[i]);
		return map;
	}

	/*
	    buf.writeLong(this.b.s());
        buf.writeBoolean(this.e);
        buf.d(this.c.length);
        for (int i2 = 0; i2 < this.c.length; ++i2) {
            buf.b((long)(Block.getCombinedId(this.d[i2]) << 12 | this.c[i2]));
        }
	*/

	/*/**
	 * Retrieve the chunk that has been altered.
	 * 
	 * @return The current chunk
	 *//*
	public ChunkCoordIntPair getChunk() {
		return handle.getChunkCoordIntPairs().read(0);
	}

	/**
	 * Set the chunk that has been altered.
	 * 
	 * @param value - new value
	 *//*
	public void setChunk(ChunkCoordIntPair value) {
		handle.getChunkCoordIntPairs().write(0, value);
	}

	/**
	 * Retrieve a copy of the record data as a block change array.
	 * 
	 * @return The copied block change array.
	 *//*
	public MultiBlockChangeInfo[] getRecords() {
		return handle.getMultiBlockChangeInfoArrays().read(0);
	}

	/**
	 * Set the record data using the given helper array.
	 * 
	 * @param value - new value
	 *//*
	public void setRecords(MultiBlockChangeInfo[] value) {
		handle.getMultiBlockChangeInfoArrays().write(0, value);
	}*/
}
