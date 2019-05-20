package com.primestone.wallet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.coinomi.core.coins.CoinType;
import com.coinomi.core.wallet.AbstractAddress;

import java.util.List;

import javax.annotation.Nonnull;

public class AddressBookProvider extends ContentProvider {
    private static final String DATABASE_TABLE = "address_book";

    public static final String KEY_ROWID = "_id";
    public static final String KEY_COIN_ID = "coin_id";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_LABEL = "label";

    public static final String SELECTION_QUERY = "q";
    public static final String SELECTION_IN = "in";
    public static final String SELECTION_NOTIN = "notin";

    public static Uri contentUri(@Nonnull final String packageName) {
        return contentUri(packageName, "");
    }

    public static Uri contentUri(@Nonnull final String packageName, @Nonnull final CoinType type) {
        return contentUri(packageName, type.getId());
    }

    private static Uri contentUri(@Nonnull final String packageName, @Nonnull final String coinId) {
        return Uri.parse("content://" + packageName + '.' + DATABASE_TABLE + "/" + coinId);
    }

    public static String resolveLabel(final Context context, final AbstractAddress address) {
        String label = null;

        if (context != null) {
            final Uri uri = contentUri(context.getPackageName(), address.getType())
                    .buildUpon().appendPath(address.toString()).build();
            final Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
//            Cursor cursor = context.getContentResolver().query(contentUri(context.getPackageName(), address.getType()).buildUpon().appendPath(address.toString()).build(), null, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    label = cursor.getString(cursor.getColumnIndexOrThrow(AddressBookProvider.KEY_LABEL));
                }

                cursor.close();
            }
        }

        return label;
    }

    private Helper helper;


    @Override
    public boolean onCreate() {
        helper = new Helper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final List<String> pathSegments = getPathSegments(uri);

        final String coinId = (String) pathSegments.get(0);
        final String address = (String) pathSegments.get(1);
        values.put("coin_id", coinId);
        values.put("address", address);

        final Uri rowUri = contentUri(getContext().getPackageName(), coinId).buildUpon().appendPath(address).appendPath(Long.toString(this.helper.getWritableDatabase().insertOrThrow("address_book", null, values))).build();

        getContext().getContentResolver().notifyChange(rowUri, null);

        return rowUri;
    }

    private List<String> getPathSegments(Uri uri) {
        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() == 2) {
            return pathSegments;
        }
        throw new IllegalArgumentException(uri.toString());
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        final List<String> pathSegments = getPathSegments(uri);

        final String coinId = pathSegments.get(0);
        final String address = pathSegments.get(1);
        values.put("coin_id", coinId);
        values.put("address", address);

        final int count = helper.getWritableDatabase().update("address_book", values, "coin_id=? AND address=?", new String[]{coinId, address});
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        List<String> pathSegments = getPathSegments(uri);

        final String coinId = (String) pathSegments.get(0);
        final String address = (String) pathSegments.get(1);

        final int count = helper.getWritableDatabase().delete("address_book", "coin_id=? AND address=?", new String[]{coinId, address});

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String originalSelection,
                        final String[] originalSelectionArgs, final String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DATABASE_TABLE);

        final List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.size() < 1 || pathSegments.size() > 2) {
            throw new IllegalArgumentException(uri.toString());
        }

        String selection = null;
        String[] selectionArgs = null;
        final String coinId = (String) pathSegments.get(0);

        qb.appendWhere(KEY_COIN_ID + "=");
        qb.appendWhereEscapeString(coinId);

        if (pathSegments.size() == 2) {
            final String address = (String) pathSegments.get(1);

            qb.appendWhere(" AND address=");
            qb.appendWhereEscapeString(address);
        } else if ("in".equals(originalSelection)) {
            final String[] addresses = originalSelectionArgs[0].trim().split(",");
            qb.appendWhere(" AND address IN (");
            appendAddresses(qb, addresses);
            qb.appendWhere(")");
        } else if ("notin".equals(originalSelection)) {
            final String[] addresses = originalSelectionArgs[0].trim().split(",");

            qb.appendWhere(" AND address NOT IN (");
            appendAddresses(qb, addresses);
            qb.appendWhere(")");
        } else if ("q".equals(originalSelection)) {
            final String query = '%' + originalSelectionArgs[0].trim() + '%';
            selection = "address LIKE ? OR label LIKE ?";
            selectionArgs = new String[]{query, query};
        }

        final Cursor cursor = qb.query(helper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    private static void appendAddresses(@Nonnull final SQLiteQueryBuilder qb, @Nonnull final String[] addresses) {
        for (final String address : addresses) {
            qb.appendWhereEscapeString(address.trim());
            if (!address.equals(addresses[addresses.length - 1])) {
                qb.appendWhere(",");
            }
        }
    }

    private static class Helper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "address_book";
        private static final int DATABASE_VERSION = 1;

        private static final String DATABASE_CREATE = "CREATE TABLE " + DATABASE_TABLE + " (" //
                + KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " //
                + KEY_COIN_ID + " TEXT NOT NULL, " //
                + KEY_ADDRESS + " TEXT NOT NULL, " //
                + KEY_LABEL + " TEXT NULL);";

        public Helper(final Context context) {
            super(context, "address_book", null, 1);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            db.beginTransaction();
//            int v = oldVersion;
//            while (v < newVersion) {
//                try {
//                    upgrade(db, v);
//                    v++;
//                } catch (Throwable th) {
//                    db.endTransaction();
//                }
//            }
//            db.setTransactionSuccessful();
//            db.endTransaction();
            try {
                for (int v = oldVersion; v < newVersion; v++)
                    upgrade(db, v);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        private void upgrade(final SQLiteDatabase db, final int oldVersion) {
            if (oldVersion == 1) {
                // future
            } else {
                throw new UnsupportedOperationException("old=" + oldVersion);
            }
        }
    }
}
