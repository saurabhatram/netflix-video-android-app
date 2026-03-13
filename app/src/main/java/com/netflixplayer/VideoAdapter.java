package com.netflixplayer;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context;
    private List<VideoItem> videoList;
    private OnVideoClickListener listener;
    private boolean showProgress;

    public interface OnVideoClickListener {
        void onVideoClick(VideoItem video, int position);
        void onVideoLongClick(VideoItem video, int position);
    }

    public VideoAdapter(Context context, List<VideoItem> videoList,
                        OnVideoClickListener listener, boolean showProgress) {
        this.context = context;
        this.videoList = videoList;
        this.listener = listener;
        this.showProgress = showProgress;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_card, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);

        // Title
        holder.tvTitle.setText(video.getTitle());

        // Duration
        holder.tvDuration.setText(video.getFormattedDuration());

        // New badge
        if (video.isNew()) {
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvBadge.setVisibility(View.GONE);
        }

        // Watch progress for continue watching
        if (showProgress && video.getWatchProgress() > 0) {
            holder.progressWatched.setVisibility(View.VISIBLE);
            holder.progressWatched.setProgress(video.getWatchProgress());
        } else {
            holder.progressWatched.setVisibility(View.GONE);
        }

        // Load thumbnail via Glide
        Uri videoUri = Uri.parse(video.getPath());
        Glide.with(context)
                .load(videoUri)
                .placeholder(R.drawable.placeholder_video)
                .error(R.drawable.placeholder_video)
                .transform(new CenterCrop(), new RoundedCorners(12))
                .into(holder.ivThumbnail);

        // Animate card appearance
        holder.cardView.setAlpha(0f);
        holder.cardView.animate().alpha(1f).setDuration(300).start();

        // Click listeners
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(video, holder.getAdapterPosition());
            }
        });

        holder.cardView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onVideoLongClick(video, holder.getAdapterPosition());
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return videoList != null ? videoList.size() : 0;
    }

    public void updateList(List<VideoItem> newList) {
        this.videoList = newList;
        notifyDataSetChanged();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvDuration;
        TextView tvBadge;
        ProgressBar progressWatched;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvBadge = itemView.findViewById(R.id.tv_badge);
            progressWatched = itemView.findViewById(R.id.progress_watched);
        }
    }
}
